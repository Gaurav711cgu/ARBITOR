package com.arbiter.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record JournalEntry(
        String accountId,
        String transactionId,
        EntryType entryType,
        BigDecimal amount,
        String currency,
        BigDecimal balanceAfter,
        String counterpartyAccount,
        String purposeCode,
        Instant timestamp
) {
    public String key() {
        return "ACCOUNT#" + accountId + "#TXN#" + transactionId + "#" + entryType;
    }
}

