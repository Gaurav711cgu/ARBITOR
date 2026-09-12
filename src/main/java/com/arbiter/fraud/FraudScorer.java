package com.arbiter.fraud;

import com.arbiter.domain.FraudScore;
import com.arbiter.domain.PaymentInstruction;

public interface FraudScorer {
    FraudScore score(PaymentInstruction payment);
}

