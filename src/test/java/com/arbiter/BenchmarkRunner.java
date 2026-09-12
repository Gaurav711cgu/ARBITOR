package com.arbiter;

import com.arbiter.audit.AuditEventPublisher;
import com.arbiter.domain.Account;
import com.arbiter.domain.JournalEntry;
import com.arbiter.domain.PaymentInstruction;
import com.arbiter.domain.SettlementResult;
import com.arbiter.domain.SettlementStatus;
import com.arbiter.fraud.RuleBasedFraudScorer;
import com.arbiter.ledger.FileLedgerRepository;
import com.arbiter.ledger.LedgerService;
import com.arbiter.ledger.ReconciliationReport;
import com.arbiter.ledger.ReconciliationService;
import com.arbiter.settlement.SettlementSaga;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class BenchmarkRunner {
    private static final int SETTLEMENTS = Integer.getInteger("arbiter.benchmark.settlements", 10_000);
    private static final int DUPLICATE_ATTEMPTS = Integer.getInteger("arbiter.benchmark.duplicates", 10_000);
    private static final int FRAUD_DECLINES = Integer.getInteger("arbiter.benchmark.fraudDeclines", 1_000);
    private static final int WORKERS = Integer.getInteger("arbiter.benchmark.workers", Math.max(4, Runtime.getRuntime().availableProcessors()));

    public static void main(String[] args) throws Exception {
        Path resultDir = Path.of("benchmark-results");
        Files.createDirectories(resultDir);
        Path dataDir = Files.createTempDirectory("arbiter-benchmark-ledger-");

        FileLedgerRepository repository = new FileLedgerRepository(dataDir);
        repository.createAccount(new Account("BENCH_DEBTOR", "USD", new BigDecimal("25000000.00")));
        repository.createAccount(new Account("BENCH_CREDITOR", "USD", new BigDecimal("0.00")));
        AuditEventPublisher audit = new AuditEventPublisher(dataDir.resolve("audit.log"));
        SettlementSaga saga = new SettlementSaga(repository, new LedgerService(repository, audit), new RuleBasedFraudScorer(repository), audit);

        List<Long> latenciesMicros = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger settled = new AtomicInteger();
        AtomicInteger duplicateResponses = new AtomicInteger();
        AtomicInteger fraudDeclined = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();

        long settlementStarted = System.nanoTime();
        runConcurrently(SETTLEMENTS, WORKERS, index -> {
            long started = System.nanoTime();
            try {
                SettlementResult result = saga.execute(new PaymentInstruction(
                        "BENCH-" + index,
                        "BENCH_DEBTOR",
                        "BENCH_CREDITOR",
                        new BigDecimal("1.00"),
                        "USD",
                        "GDDS",
                        "benchmark"
                ));
                if (result.status() == SettlementStatus.SETTLED) {
                    settled.incrementAndGet();
                } else {
                    errors.incrementAndGet();
                }
            } catch (RuntimeException ex) {
                errors.incrementAndGet();
            } finally {
                latenciesMicros.add(TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - started));
            }
        });
        long settlementElapsedNanos = System.nanoTime() - settlementStarted;

        long duplicatesStarted = System.nanoTime();
        runConcurrently(DUPLICATE_ATTEMPTS, WORKERS, ignored -> {
            SettlementResult result = saga.execute(new PaymentInstruction(
                    "BENCH-0",
                    "BENCH_DEBTOR",
                    "BENCH_CREDITOR",
                    new BigDecimal("1.00"),
                    "USD",
                    "GDDS",
                    "benchmark"
            ));
            if (result.status() == SettlementStatus.DUPLICATE) {
                duplicateResponses.incrementAndGet();
            } else {
                errors.incrementAndGet();
            }
        });
        long duplicatesElapsedNanos = System.nanoTime() - duplicatesStarted;

        long fraudStarted = System.nanoTime();
        runConcurrently(FRAUD_DECLINES, WORKERS, index -> {
            SettlementResult result = saga.execute(new PaymentInstruction(
                    "FRAUD-BENCH-" + index,
                    "BENCH_DEBTOR",
                    "BENCH_CREDITOR",
                    new BigDecimal("20000.00"),
                    "USD",
                    "GDDS",
                    "benchmark"
            ));
            if (result.status() == SettlementStatus.DECLINED) {
                fraudDeclined.incrementAndGet();
            } else {
                errors.incrementAndGet();
            }
        });
        long fraudElapsedNanos = System.nanoTime() - fraudStarted;

        ReconciliationReport report = new ReconciliationService(repository).reconcile();
        BigDecimal globalSum = repository.allEntries().stream()
                .map(JournalEntry::amount)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);

        BenchmarkStats stats = new BenchmarkStats(
                Instant.now().toString(),
                SETTLEMENTS,
                settled.get(),
                DUPLICATE_ATTEMPTS,
                duplicateResponses.get(),
                FRAUD_DECLINES,
                fraudDeclined.get(),
                errors.get(),
                WORKERS,
                nanosToSeconds(settlementElapsedNanos),
                rate(settled.get(), settlementElapsedNanos),
                percentile(latenciesMicros, 50),
                percentile(latenciesMicros, 95),
                percentile(latenciesMicros, 99),
                nanosToSeconds(duplicatesElapsedNanos),
                rate(duplicateResponses.get(), duplicatesElapsedNanos),
                nanosToSeconds(fraudElapsedNanos),
                rate(fraudDeclined.get(), fraudElapsedNanos),
                repository.allEntries().size(),
                globalSum.toPlainString(),
                report.balanced()
        );

        String json = stats.toJson();
        String text = stats.toText();
        Files.writeString(resultDir.resolve("benchmark-latest.json"), json);
        Files.writeString(resultDir.resolve("benchmark-latest.txt"), text);
        System.out.print(text);

        if (stats.errors != 0 || !stats.reconciliationBalanced || stats.settled != stats.requestedSettlements || stats.duplicateResponses != stats.duplicateAttempts || stats.fraudDeclines != stats.requestedFraudDeclines) {
            throw new AssertionError("benchmark acceptance failed; see benchmark-results");
        }
    }

    private static void runConcurrently(int totalTasks, int workers, ThrowingIntConsumer task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger cursor = new AtomicInteger();
        for (int worker = 0; worker < workers; worker++) {
            executor.submit(() -> {
                try {
                    start.await();
                    int index;
                    while ((index = cursor.getAndIncrement()) < totalTasks) {
                        task.accept(index);
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        }
        start.countDown();
        executor.shutdown();
        if (!executor.awaitTermination(15, TimeUnit.MINUTES)) {
            throw new AssertionError("benchmark timed out");
        }
    }

    private static long percentile(List<Long> values, int percentile) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = Math.min(sorted.size() - 1, (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1);
        return sorted.get(Math.max(index, 0));
    }

    private static double nanosToSeconds(long nanos) {
        return nanos / 1_000_000_000.0;
    }

    private static double rate(int count, long nanos) {
        return count / nanosToSeconds(nanos);
    }

    @FunctionalInterface
    private interface ThrowingIntConsumer {
        void accept(int value) throws Exception;
    }

    private record BenchmarkStats(
            String completedAt,
            int requestedSettlements,
            int settled,
            int duplicateAttempts,
            int duplicateResponses,
            int requestedFraudDeclines,
            int fraudDeclines,
            int errors,
            int workers,
            double settlementSeconds,
            double settlementsPerSecond,
            long p50Micros,
            long p95Micros,
            long p99Micros,
            double duplicateSeconds,
            double duplicateResponsesPerSecond,
            double fraudSeconds,
            double fraudDeclinesPerSecond,
            int journalEntryCount,
            String globalJournalSum,
            boolean reconciliationBalanced
    ) {
        String toJson() {
            return "{\n"
                    + "  \"completedAt\": \"" + completedAt + "\",\n"
                    + "  \"requestedSettlements\": " + requestedSettlements + ",\n"
                    + "  \"settled\": " + settled + ",\n"
                    + "  \"duplicateAttempts\": " + duplicateAttempts + ",\n"
                    + "  \"duplicateResponses\": " + duplicateResponses + ",\n"
                    + "  \"requestedFraudDeclines\": " + requestedFraudDeclines + ",\n"
                    + "  \"fraudDeclines\": " + fraudDeclines + ",\n"
                    + "  \"errors\": " + errors + ",\n"
                    + "  \"workers\": " + workers + ",\n"
                    + "  \"settlementSeconds\": " + format(settlementSeconds) + ",\n"
                    + "  \"settlementsPerSecond\": " + format(settlementsPerSecond) + ",\n"
                    + "  \"p50Micros\": " + p50Micros + ",\n"
                    + "  \"p95Micros\": " + p95Micros + ",\n"
                    + "  \"p99Micros\": " + p99Micros + ",\n"
                    + "  \"duplicateSeconds\": " + format(duplicateSeconds) + ",\n"
                    + "  \"duplicateResponsesPerSecond\": " + format(duplicateResponsesPerSecond) + ",\n"
                    + "  \"fraudSeconds\": " + format(fraudSeconds) + ",\n"
                    + "  \"fraudDeclinesPerSecond\": " + format(fraudDeclinesPerSecond) + ",\n"
                    + "  \"journalEntryCount\": " + journalEntryCount + ",\n"
                    + "  \"globalJournalSum\": \"" + globalJournalSum + "\",\n"
                    + "  \"reconciliationBalanced\": " + reconciliationBalanced + "\n"
                    + "}\n";
        }

        String toText() {
            return """
                    Arbiter Durable Ledger Benchmark
                    =================================
                    completedAt: %s
                    workers: %d
                    durableStore: append-only file ledger with checksum and fsync

                    settlementRequests: %d
                    settled: %d
                    settlementSeconds: %s
                    settlementsPerSecond: %s
                    latencyMicrosP50: %d
                    latencyMicrosP95: %d
                    latencyMicrosP99: %d

                    duplicateAttempts: %d
                    duplicateResponses: %d
                    duplicateSeconds: %s
                    duplicateResponsesPerSecond: %s

                    fraudDeclineRequests: %d
                    fraudDeclines: %d
                    fraudSeconds: %s
                    fraudDeclinesPerSecond: %s

                    journalEntryCount: %d
                    globalJournalSum: %s
                    reconciliationBalanced: %s
                    errors: %d
                    """.formatted(
                    completedAt,
                    workers,
                    requestedSettlements,
                    settled,
                    format(settlementSeconds),
                    format(settlementsPerSecond),
                    p50Micros,
                    p95Micros,
                    p99Micros,
                    duplicateAttempts,
                    duplicateResponses,
                    format(duplicateSeconds),
                    format(duplicateResponsesPerSecond),
                    requestedFraudDeclines,
                    fraudDeclines,
                    format(fraudSeconds),
                    format(fraudDeclinesPerSecond),
                    journalEntryCount,
                    globalJournalSum,
                    reconciliationBalanced,
                    errors
            );
        }

        private static String format(double value) {
            return String.format(Locale.ROOT, "%.3f", value);
        }
    }
}
