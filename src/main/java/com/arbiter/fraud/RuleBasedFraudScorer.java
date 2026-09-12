package com.arbiter.fraud;

import com.arbiter.domain.FraudDecision;
import com.arbiter.domain.FraudScore;
import com.arbiter.domain.PaymentInstruction;
import com.arbiter.ledger.LedgerRepository;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class RuleBasedFraudScorer implements FraudScorer {
    private static final BigDecimal HARD_DECLINE_AMOUNT = new BigDecimal("10000.00");
    private static final BigDecimal REVIEW_AMOUNT = new BigDecimal("5000.00");
    private final LedgerRepository repository;

    public RuleBasedFraudScorer(LedgerRepository repository) {
        this.repository = repository;
    }

    @Override
    public FraudScore score(PaymentInstruction payment) {
        List<String> reasons = new ArrayList<>();
        double score = 0.05;

        if (payment.amount().compareTo(HARD_DECLINE_AMOUNT) > 0) {
            score += 0.86;
            reasons.add("amount above hard decline threshold");
        } else if (payment.amount().compareTo(REVIEW_AMOUNT) > 0) {
            score += 0.45;
            reasons.add("amount above review threshold");
        }

        int priorEntries = repository.entriesForAccount(payment.debtorAccount()).size();
        if (priorEntries == 0 && payment.amount().compareTo(new BigDecimal("1000.00")) > 0) {
            score += 0.20;
            reasons.add("first outgoing transfer is unusually large");
        }

        int hour = LocalTime.now().getHour();
        if (hour < 5) {
            score += 0.10;
            reasons.add("unusual local hour");
        }

        FraudDecision decision = score > 0.85
                ? FraudDecision.DECLINE
                : score > 0.50 ? FraudDecision.REVIEW : FraudDecision.APPROVE;

        if (reasons.isEmpty()) {
            reasons.add("no rule triggered");
        }
        return new FraudScore(Math.min(score, 0.99), decision, List.copyOf(reasons));
    }
}

