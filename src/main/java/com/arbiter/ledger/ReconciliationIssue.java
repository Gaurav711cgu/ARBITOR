package com.arbiter.ledger;

import java.math.BigDecimal;

public record ReconciliationIssue(
        String accountId,
        BigDecimal expectedBalance,
        BigDecimal actualBalance,
        String reason
) {
}

