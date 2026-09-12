package com.arbiter.ledger;

import com.arbiter.domain.Account;
import com.arbiter.domain.JournalEntry;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface LedgerRepository {
    void createAccount(Account account);

    Optional<Account> findAccount(String accountId);

    List<Account> allAccounts();

    boolean hasTransaction(String transactionId);

    List<JournalEntry> entriesForTransaction(String transactionId);

    List<JournalEntry> entriesForAccount(String accountId);

    List<JournalEntry> allEntries();

    List<String> getRecentTransactions();

    void postAtomically(String transactionId, JournalEntry debitEntry, JournalEntry creditEntry);

    void assertCanDebit(String accountId, BigDecimal amount, String currency);
}
