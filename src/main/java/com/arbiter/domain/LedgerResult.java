package com.arbiter.domain;

import java.util.List;

public record LedgerResult(String transactionId, LedgerStatus status, List<JournalEntry> entries) {
    public static LedgerResult duplicate(String transactionId, List<JournalEntry> entries) {
        return new LedgerResult(transactionId, LedgerStatus.DUPLICATE, entries);
    }

    public static LedgerResult posted(String transactionId, List<JournalEntry> entries) {
        return new LedgerResult(transactionId, LedgerStatus.POSTED, entries);
    }
}

