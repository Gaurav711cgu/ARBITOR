package com.arbiter.domain;

import java.util.List;

public record FraudScore(double value, FraudDecision decision, List<String> reasons) {
    public boolean declined() {
        return decision == FraudDecision.DECLINE;
    }
}

