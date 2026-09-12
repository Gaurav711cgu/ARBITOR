package com.arbiter;

import com.arbiter.audit.AuditEventPublisher;
import com.arbiter.api.ApiAuth;
import com.arbiter.api.Json;
import com.arbiter.domain.Account;
import com.arbiter.domain.EntryType;
import com.arbiter.domain.JournalEntry;
import com.arbiter.domain.PaymentInstruction;
import com.arbiter.domain.SettlementResult;
import com.arbiter.domain.SettlementStatus;
import com.arbiter.fraud.RuleBasedFraudScorer;
import com.arbiter.iso20022.Iso20022MessageParser;
import com.arbiter.ledger.DuplicateTransactionException;
import com.arbiter.ledger.FileLedgerRepository;
import com.arbiter.ledger.LedgerException;
import com.arbiter.ledger.LedgerService;
import com.arbiter.ledger.ReconciliationService;
import com.arbiter.settlement.SettlementSaga;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ArbiterTestRunner {
    public static void main(String[] args) {
        testDoubleEntryInvariantUnderRandomTransactions();
        testIdempotencyPreventsDuplicateSettlement();
        testJournalEntriesAreImmutable();
        testFraudDeclineHappensBeforeSettlement();
        testIso20022ParserUsesEndToEndId();
        testConcurrentDebitsCannotOverdrawAccount();
        testConcurrentDuplicateSubmissionsPostOnce();
        testDurableRecoveryFromLedgerFile();
        testDurableAuditRecovery();
        testCorruptedLedgerFailsReplay();
        testJsonParserHandlesEscapesAndRejectsNestedBodies();
        testReconciliationDetectsCleanLedger();
        testApiKeyAuthAcceptsOnlyExactSecret();
        System.out.println("All Arbiter tests passed");
    }

    private static void testDoubleEntryInvariantUnderRandomTransactions() {
        Harness harness = harness();
        Random random = new Random(42);
        for (int i = 0; i < 2_000; i++) {
            BigDecimal amount = new BigDecimal(random.nextInt(20) + 1).setScale(2);
            String debtor = random.nextBoolean() ? "A" : "B";
            String creditor = debtor.equals("A") ? "B" : "A";
            harness.saga.execute(new PaymentInstruction("RND-" + i, debtor, creditor, amount, "USD", "GDDS", "test"));
        }

        BigDecimal globalSum = harness.repository.allEntries().stream()
                .map(JournalEntry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(BigDecimal.ZERO.setScale(2), globalSum, "global journal sum must stay zero");
        assertTrue(harness.repository.allEntries().size() == 4_000, "each settlement writes two entries");
    }

    private static void testIdempotencyPreventsDuplicateSettlement() {
        Harness harness = harness();
        PaymentInstruction payment = new PaymentInstruction("IDEMP-1", "A", "B", new BigDecimal("25.00"), "USD", "GDDS", "test");
        SettlementResult first = harness.saga.execute(payment);
        SettlementResult second = harness.saga.execute(payment);

        assertEquals(SettlementStatus.SETTLED, first.status(), "first payment settles");
        assertEquals(SettlementStatus.DUPLICATE, second.status(), "duplicate payment returns duplicate status");
        assertTrue(harness.repository.entriesForTransaction("IDEMP-1").size() == 2, "duplicate must not add journal legs");
    }

    private static void testJournalEntriesAreImmutable() {
        Harness harness = harness();
        harness.saga.execute(new PaymentInstruction("IMM-1", "A", "B", new BigDecimal("10.00"), "USD", "GDDS", "test"));
        JournalEntry original = harness.repository.entriesForTransaction("IMM-1").get(0);
        JournalEntry replacement = new JournalEntry(
                original.accountId(),
                original.transactionId(),
                original.entryType(),
                new BigDecimal("999.00"),
                original.currency(),
                original.balanceAfter(),
                original.counterpartyAccount(),
                original.purposeCode(),
                Instant.now()
        );

        assertThrows(DuplicateTransactionException.class, () -> harness.repository.attemptOverwriteForTest(replacement), "overwrite must fail");
    }

    private static void testFraudDeclineHappensBeforeSettlement() {
        Harness harness = harness();
        SettlementResult result = harness.saga.execute(new PaymentInstruction("FRAUD-1", "A", "B", new BigDecimal("20000.00"), "USD", "GDDS", "test"));
        assertEquals(SettlementStatus.DECLINED, result.status(), "large transfer is declined");
        assertTrue(harness.repository.entriesForTransaction("FRAUD-1").isEmpty(), "declined payment posts no journal entries");
    }

    private static void testIso20022ParserUsesEndToEndId() {
        String xml = """
                <Document>
                  <CstmrCdtTrfInitn>
                    <PmtInf>
                      <CdtTrfTxInf>
                        <PmtId><EndToEndId>E2E-ISO-1</EndToEndId></PmtId>
                        <Amt><InstdAmt Ccy="USD">12.34</InstdAmt></Amt>
                        <Dbtr><Nm>A</Nm></Dbtr>
                        <Cdtr><Nm>B</Nm></Cdtr>
                        <Purp><Cd>GDDS</Cd></Purp>
                      </CdtTrfTxInf>
                    </PmtInf>
                  </CstmrCdtTrfInitn>
                </Document>
                """;
        PaymentInstruction payment = new Iso20022MessageParser().parsePain001(xml);
        assertEquals("E2E-ISO-1", payment.endToEndId(), "ISO parser must extract EndToEndId");
        assertEquals(new BigDecimal("12.34"), payment.amount(), "ISO parser must extract instructed amount");
    }

    private static void testConcurrentDebitsCannotOverdrawAccount() {
        Path directory = tempDirectory();
        FileLedgerRepository repository = new FileLedgerRepository(directory);
        repository.createAccount(new Account("A", "USD", new BigDecimal("100.00")));
        repository.createAccount(new Account("B", "USD", new BigDecimal("0.00")));
        AuditEventPublisher audit = new AuditEventPublisher(directory.resolve("audit.log"));
        SettlementSaga saga = new SettlementSaga(repository, new LedgerService(repository, audit), new RuleBasedFraudScorer(repository), audit);

        runConcurrently(32, index -> {
            try {
                saga.execute(new PaymentInstruction("CONC-" + index, "A", "B", new BigDecimal("10.00"), "USD", "GDDS", "test"));
            } catch (RuntimeException ignored) {
                // Expected once available funds are exhausted.
            }
        });

        BigDecimal balance = repository.findAccount("A").orElseThrow().balance();
        assertEquals(BigDecimal.ZERO.setScale(2), balance, "atomic debit must stop exactly at zero");
        assertTrue(repository.allEntries().size() == 20, "only 10 successful payments should post 20 journal entries");
    }

    private static void testConcurrentDuplicateSubmissionsPostOnce() {
        Harness harness = harness();
        runConcurrently(40, ignored -> {
            SettlementResult result = harness.saga.execute(new PaymentInstruction("DUP-RACE", "A", "B", new BigDecimal("1.00"), "USD", "GDDS", "test"));
            assertTrue(result.status() == SettlementStatus.SETTLED || result.status() == SettlementStatus.DUPLICATE, "duplicate race must return a clean settlement status");
        });
        assertTrue(harness.repository.entriesForTransaction("DUP-RACE").size() == 2, "duplicate race must post exactly one double-entry pair");
    }

    private static void testDurableRecoveryFromLedgerFile() {
        Path directory = tempDirectory();
        FileLedgerRepository repository = new FileLedgerRepository(directory);
        repository.createAccount(new Account("A", "USD", new BigDecimal("50.00")));
        repository.createAccount(new Account("B", "USD", new BigDecimal("0.00")));
        AuditEventPublisher audit = new AuditEventPublisher(directory.resolve("audit.log"));
        SettlementSaga saga = new SettlementSaga(repository, new LedgerService(repository, audit), new RuleBasedFraudScorer(repository), audit);
        saga.execute(new PaymentInstruction("RECOVER-1", "A", "B", new BigDecimal("12.00"), "USD", "GDDS", "test"));

        FileLedgerRepository recovered = new FileLedgerRepository(directory);
        assertEquals(new BigDecimal("38.00"), recovered.findAccount("A").orElseThrow().balance(), "recovered debtor balance must match disk ledger");
        assertEquals(new BigDecimal("12.00"), recovered.findAccount("B").orElseThrow().balance(), "recovered creditor balance must match disk ledger");
        assertTrue(recovered.entriesForTransaction("RECOVER-1").size() == 2, "recovered transaction must have both legs");
    }

    private static void testDurableAuditRecovery() {
        Path directory = tempDirectory();
        FileLedgerRepository repository = new FileLedgerRepository(directory);
        repository.createAccount(new Account("A", "USD", new BigDecimal("100.00")));
        repository.createAccount(new Account("B", "USD", new BigDecimal("0.00")));
        Path auditPath = directory.resolve("audit.log");
        AuditEventPublisher audit = new AuditEventPublisher(auditPath);
        SettlementSaga saga = new SettlementSaga(repository, new LedgerService(repository, audit), new RuleBasedFraudScorer(repository), audit);
        saga.execute(new PaymentInstruction("AUDIT-1", "A", "B", new BigDecimal("10.00"), "USD", "GDDS", "test"));

        AuditEventPublisher recovered = new AuditEventPublisher(auditPath);
        assertTrue(recovered.events().size() == 6, "audit log must recover all settlement events");
    }

    private static void testCorruptedLedgerFailsReplay() {
        Path directory = tempDirectory();
        FileLedgerRepository repository = new FileLedgerRepository(directory);
        repository.createAccount(new Account("A", "USD", new BigDecimal("100.00")));
        repository.createAccount(new Account("B", "USD", new BigDecimal("0.00")));
        AuditEventPublisher audit = new AuditEventPublisher(directory.resolve("audit.log"));
        SettlementSaga saga = new SettlementSaga(repository, new LedgerService(repository, audit), new RuleBasedFraudScorer(repository), audit);
        saga.execute(new PaymentInstruction("CORRUPT-1", "A", "B", new BigDecimal("10.00"), "USD", "GDDS", "test"));

        try {
            Path ledger = directory.resolve("ledger.log");
            String tampered = Files.readString(ledger).replace("-10.00", "-11.00");
            Files.writeString(ledger, tampered);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        assertThrows(LedgerException.class, () -> new FileLedgerRepository(directory), "corrupted ledger checksum must fail replay");
    }

    private static void testJsonParserHandlesEscapesAndRejectsNestedBodies() {
        assertEquals("ACCT,\"A\"", Json.parseFlatObject("{\"accountId\":\"ACCT,\\\"A\\\"\",\"currency\":\"USD\"}").get("accountId"), "parser must handle comma and quote inside string");
        assertThrows(IllegalArgumentException.class, () -> Json.parseFlatObject("{\"account\":{\"id\":\"A\"}}"), "nested JSON must be rejected explicitly");
    }

    private static void testReconciliationDetectsCleanLedger() {
        Harness harness = harness();
        harness.saga.execute(new PaymentInstruction("RECON-1", "A", "B", new BigDecimal("10.00"), "USD", "GDDS", "test"));
        assertTrue(new ReconciliationService(harness.repository).reconcile().balanced(), "clean ledger must reconcile");
    }

    private static void testApiKeyAuthAcceptsOnlyExactSecret() {
        ApiAuth auth = ApiAuth.fromKey("test-key-that-is-long-enough");
        assertTrue(auth.accepts("test-key-that-is-long-enough"), "exact API key must authenticate");
        assertTrue(!auth.accepts("wrong-key-that-is-long-enough"), "wrong API key must fail");
        assertTrue(!auth.accepts(null), "missing API key must fail");
    }

    private static Harness harness() {
        Path directory = tempDirectory();
        FileLedgerRepository repository = new FileLedgerRepository(directory);
        repository.createAccount(new Account("A", "USD", new BigDecimal("100000.00")));
        repository.createAccount(new Account("B", "USD", new BigDecimal("100000.00")));
        AuditEventPublisher audit = new AuditEventPublisher(directory.resolve("audit.log"));
        LedgerService ledger = new LedgerService(repository, audit);
        SettlementSaga saga = new SettlementSaga(repository, ledger, new RuleBasedFraudScorer(repository), audit);
        return new Harness(repository, saga);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static void assertThrows(Class<? extends Throwable> expected, Runnable runnable, String message) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if (expected.isInstance(throwable)) {
                return;
            }
            throw new AssertionError(message + ": expected " + expected.getSimpleName() + " but got " + throwable.getClass().getSimpleName());
        }
        throw new AssertionError(message + ": expected exception " + expected.getSimpleName());
    }

    private static void runConcurrently(int workers, ThrowingIntConsumer task) {
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger();
        List<Throwable> errors = java.util.Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < workers; i++) {
            int index = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    task.accept(index);
                } catch (Throwable throwable) {
                    failures.incrementAndGet();
                    errors.add(throwable);
                }
            });
        }
        try {
            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                throw new AssertionError("concurrent test timed out");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("concurrent test interrupted");
        }
        if (failures.get() > 0) {
            throw new AssertionError("unexpected concurrent test failure: " + errors.get(0));
        }
    }

    @FunctionalInterface
    private interface ThrowingIntConsumer {
        void accept(int value) throws Exception;
    }

    private static Path tempDirectory() {
        try {
            return Files.createTempDirectory("arbiter-test-");
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record Harness(FileLedgerRepository repository, SettlementSaga saga) {
    }
}
