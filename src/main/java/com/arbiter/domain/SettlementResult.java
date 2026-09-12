package com.arbiter.domain;

import java.util.List;

public record SettlementResult(
        String transactionId,
        SettlementStatus status,
        FraudScore fraudScore,
        LedgerResult ledgerResult,
        List<String> auditEvents
) {
}

