package com.arbiter.ledger;

import java.math.BigDecimal;
import java.util.List;

public record ReconciliationReport(
        boolean balanced,
        BigDecimal globalJournalSum,
        int entryCount,
        int accountCount,
        List<ReconciliationIssue> issues
) {
}

