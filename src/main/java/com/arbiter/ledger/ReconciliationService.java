package com.arbiter.ledger;

import com.arbiter.domain.Account;
import com.arbiter.domain.JournalEntry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ReconciliationService {
    private final LedgerRepository repository;

    public ReconciliationService(LedgerRepository repository) {
        this.repository = repository;
    }

    public ReconciliationReport reconcile() {
        List<JournalEntry> entries = repository.allEntries();
        BigDecimal globalSum = entries.stream()
                .map(JournalEntry::amount)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);

        List<ReconciliationIssue> issues = new ArrayList<>();
        for (Account account : repository.allAccounts()) {
            BigDecimal journalDelta = repository.entriesForAccount(account.accountId()).stream()
                    .map(JournalEntry::amount)
                    .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
            BigDecimal expected = account.openingBalance().add(journalDelta);
            if (expected.compareTo(account.balance()) != 0) {
                issues.add(new ReconciliationIssue(account.accountId(), expected, account.balance(), "balance does not equal opening balance plus journal entries"));
            }
        }

        boolean balanced = globalSum.compareTo(BigDecimal.ZERO) == 0 && issues.isEmpty();
        return new ReconciliationReport(balanced, globalSum, entries.size(), repository.allAccounts().size(), List.copyOf(issues));
    }
}

