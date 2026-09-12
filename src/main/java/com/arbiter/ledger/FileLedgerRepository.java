package com.arbiter.ledger;

import com.arbiter.domain.Account;
import com.arbiter.domain.EntryType;
import com.arbiter.domain.JournalEntry;

import java.io.IOException;
import java.io.BufferedReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.CRC32;

public final class FileLedgerRepository implements LedgerRepository {
    private final Path ledgerFile;
    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, JournalEntry> entriesByKey = new HashMap<>();
    private final Set<String> processedTransactions = new HashSet<>();
    private final List<String> orderedTransactions = new ArrayList<>();

    public FileLedgerRepository(Path directory) {
        try {
            Files.createDirectories(directory);
            this.ledgerFile = directory.resolve("ledger.log");
            if (!Files.exists(ledgerFile)) {
                Files.createFile(ledgerFile);
            }
            replay();
        } catch (IOException ex) {
            throw new LedgerException("failed to initialize ledger store: " + ex.getMessage());
        }
    }

    @Override
    public synchronized void createAccount(Account account) {
        if (accounts.containsKey(account.accountId())) {
            throw new LedgerException("account already exists: " + account.accountId());
        }
        appendDurably(payload("ACCOUNT", account.accountId(), account.currency(), account.openingBalance().toPlainString(), account.createdAt().toString()));
        accounts.put(account.accountId(), account);
    }

    @Override
    public synchronized Optional<Account> findAccount(String accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    @Override
    public synchronized List<Account> allAccounts() {
        return new ArrayList<>(accounts.values());
    }

    @Override
    public synchronized boolean hasTransaction(String transactionId) {
        return processedTransactions.contains(transactionId);
    }

    @Override
    public synchronized List<JournalEntry> entriesForTransaction(String transactionId) {
        return entriesByKey.values().stream()
                .filter(entry -> entry.transactionId().equals(transactionId))
                .sorted(Comparator.comparing(JournalEntry::entryType))
                .toList();
    }

    @Override
    public synchronized List<JournalEntry> entriesForAccount(String accountId) {
        return entriesByKey.values().stream()
                .filter(entry -> entry.accountId().equals(accountId))
                .sorted(Comparator.comparing(JournalEntry::timestamp))
                .toList();
    }

    @Override
    public synchronized List<JournalEntry> allEntries() {
        return new ArrayList<>(entriesByKey.values());
    }

    @Override
    public synchronized void assertCanDebit(String accountId, BigDecimal amount, String currency) {
        Account account = requiredAccount(accountId);
        if (!account.currency().equalsIgnoreCase(currency)) {
            throw new LedgerException("currency mismatch for account " + accountId);
        }
        if (account.balance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("insufficient funds for account " + accountId);
        }
    }

    @Override
    public synchronized void postAtomically(String transactionId, JournalEntry debitEntry, JournalEntry creditEntry) {
        validateTransaction(transactionId, debitEntry, creditEntry);

        Account debtor = requiredAccount(debitEntry.accountId());
        Account creditor = requiredAccount(creditEntry.accountId());
        BigDecimal debtorBalanceAfter = debtor.balance().add(debitEntry.amount());
        BigDecimal creditorBalanceAfter = creditor.balance().add(creditEntry.amount());
        if (debtorBalanceAfter.signum() < 0) {
            throw new InsufficientFundsException("insufficient funds for account " + debtor.accountId());
        }

        String record = payload(
                "TXN",
                transactionId,
                debitEntry.accountId(),
                debitEntry.amount().toPlainString(),
                creditEntry.accountId(),
                creditEntry.amount().toPlainString(),
                debitEntry.currency(),
                debtorBalanceAfter.toPlainString(),
                creditorBalanceAfter.toPlainString(),
                debitEntry.counterpartyAccount(),
                creditEntry.counterpartyAccount(),
                debitEntry.purposeCode(),
                debitEntry.timestamp().toString()
        );

        appendDurably(record);
        applyTransactionRecord(record.split("\\|", -1));
    }

    
    @Override
    public synchronized List<String> getRecentTransactions() {
        int start = Math.max(0, orderedTransactions.size() - 20);
        List<String> recent = new ArrayList<>(orderedTransactions.subList(start, orderedTransactions.size()));
        Collections.reverse(recent);
        return recent;
    }

    public synchronized void attemptOverwriteForTest(JournalEntry replacement) {
        if (entriesByKey.containsKey(replacement.key())) {
            throw new DuplicateTransactionException("conditional write rejected existing key: " + replacement.key());
        }
        throw new LedgerException("file ledger does not support direct journal-entry mutation");
    }

    private void validateTransaction(String transactionId, JournalEntry debitEntry, JournalEntry creditEntry) {
        if (processedTransactions.contains(transactionId)) {
            throw new DuplicateTransactionException("duplicate transaction: " + transactionId);
        }
        if (entriesByKey.containsKey(debitEntry.key()) || entriesByKey.containsKey(creditEntry.key())) {
            throw new DuplicateTransactionException("journal entry key already exists for transaction: " + transactionId);
        }
        if (debitEntry.entryType() != EntryType.DEBIT || creditEntry.entryType() != EntryType.CREDIT) {
            throw new LedgerInvariantViolationException("double-entry legs must be DEBIT then CREDIT");
        }
        if (!debitEntry.currency().equals(creditEntry.currency())) {
            throw new LedgerException("currency mismatch between journal legs");
        }
        if (debitEntry.amount().add(creditEntry.amount()).compareTo(BigDecimal.ZERO) != 0) {
            throw new LedgerInvariantViolationException("double-entry invariant violated");
        }
        Account debtor = requiredAccount(debitEntry.accountId());
        Account creditor = requiredAccount(creditEntry.accountId());
        if (!debtor.currency().equals(debitEntry.currency()) || !creditor.currency().equals(creditEntry.currency())) {
            throw new LedgerException("currency mismatch for transaction: " + transactionId);
        }
    }

    private void replay() throws IOException {
        int lineNumber = 0;
        try (BufferedReader reader = Files.newBufferedReader(ledgerFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                String verified = verify(line, lineNumber);
                String[] fields = verified.split("\\|", -1);
                switch (fields[0]) {
                    case "ACCOUNT" -> applyAccountRecord(fields);
                    case "TXN" -> {
                        validateReplayTransaction(fields, lineNumber);
                        applyTransactionRecord(fields);
                    }
                    default -> throw new LedgerException("unknown ledger record type at line " + lineNumber);
                }
            }
        }
    }

    private void applyAccountRecord(String[] fields) {
        if (fields.length != 5) {
            throw new LedgerException("invalid account record");
        }
        Account account = new Account(fields[1], fields[2], new BigDecimal(fields[3]));
        if (accounts.putIfAbsent(account.accountId(), account) != null) {
            throw new LedgerException("duplicate account record: " + account.accountId());
        }
    }

    private void validateReplayTransaction(String[] fields, int lineNumber) {
        if (fields.length != 13) {
            throw new LedgerException("invalid transaction record at line " + lineNumber);
        }
        BigDecimal debitAmount = new BigDecimal(fields[3]);
        BigDecimal creditAmount = new BigDecimal(fields[5]);
        if (debitAmount.add(creditAmount).compareTo(BigDecimal.ZERO) != 0) {
            throw new LedgerInvariantViolationException("replay invariant failed at line " + lineNumber);
        }
        if (processedTransactions.contains(fields[1])) {
            throw new DuplicateTransactionException("duplicate transaction during replay: " + fields[1]);
        }
    }

    private void applyTransactionRecord(String[] fields) {
        String transactionId = fields[1];
        String debtorAccount = fields[2];
        BigDecimal debitAmount = new BigDecimal(fields[3]);
        String creditorAccount = fields[4];
        BigDecimal creditAmount = new BigDecimal(fields[5]);
        String currency = fields[6];
        BigDecimal debtorBalanceAfter = new BigDecimal(fields[7]);
        BigDecimal creditorBalanceAfter = new BigDecimal(fields[8]);
        String purposeCode = fields[11];
        Instant timestamp = Instant.parse(fields[12]);

        Account debtor = requiredAccount(debtorAccount);
        Account creditor = requiredAccount(creditorAccount);
        debtor.apply(debitAmount);
        creditor.apply(creditAmount);
        if (debtor.balance().compareTo(debtorBalanceAfter) != 0) {
            throw new LedgerException("debtor replay balance mismatch for transaction: " + transactionId);
        }
        if (creditor.balance().compareTo(creditorBalanceAfter) != 0) {
            throw new LedgerException("creditor replay balance mismatch for transaction: " + transactionId);
        }

        JournalEntry debit = new JournalEntry(debtorAccount, transactionId, EntryType.DEBIT, debitAmount, currency, debtorBalanceAfter, creditorAccount, purposeCode, timestamp);
        JournalEntry credit = new JournalEntry(creditorAccount, transactionId, EntryType.CREDIT, creditAmount, currency, creditorBalanceAfter, debtorAccount, purposeCode, timestamp);
        entriesByKey.put(debit.key(), debit);
        entriesByKey.put(credit.key(), credit);
        processedTransactions.add(transactionId);
        orderedTransactions.add(transactionId);
    }

    private void appendDurably(String recordPayload) {
        String line = recordPayload + "|" + checksum(recordPayload) + System.lineSeparator();
        ByteBuffer buffer = ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8));
        try (FileChannel channel = FileChannel.open(ledgerFile, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
             FileLock ignored = channel.lock()) {
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        } catch (IOException ex) {
            throw new LedgerException("failed durable ledger append: " + ex.getMessage());
        }
    }

    private String verify(String line, int lineNumber) {
        int split = line.lastIndexOf('|');
        if (split < 0) {
            throw new LedgerException("missing checksum at line " + lineNumber);
        }
        String payload = line.substring(0, split);
        String expected = line.substring(split + 1);
        String actual = checksum(payload);
        if (!actual.equals(expected)) {
            throw new LedgerException("checksum mismatch at line " + lineNumber);
        }
        return payload;
    }

    private static String payload(String... fields) {
        return String.join("|", fields);
    }

    private static String checksum(String payload) {
        CRC32 crc32 = new CRC32();
        crc32.update(payload.getBytes(StandardCharsets.UTF_8));
        return Long.toHexString(crc32.getValue());
    }

    private Account requiredAccount(String accountId) {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new LedgerException("unknown account: " + accountId);
        }
        return account;
    }
}
