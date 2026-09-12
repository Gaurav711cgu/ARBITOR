package com.arbiter.settlement;

import com.arbiter.audit.AuditEventPublisher;
import com.arbiter.domain.FraudScore;
import com.arbiter.domain.LedgerResult;
import com.arbiter.domain.LedgerStatus;
import com.arbiter.domain.PaymentInstruction;
import com.arbiter.domain.SettlementResult;
import com.arbiter.domain.SettlementStatus;
import com.arbiter.fraud.FraudScorer;
import com.arbiter.ledger.LedgerRepository;
import com.arbiter.ledger.LedgerService;

public final class SettlementSaga {
    private final LedgerRepository repository;
    private final LedgerService ledgerService;
    private final FraudScorer fraudScorer;
    private final AuditEventPublisher auditEventPublisher;

    public SettlementSaga(
            LedgerRepository repository,
            LedgerService ledgerService,
            FraudScorer fraudScorer,
            AuditEventPublisher auditEventPublisher
    ) {
        this.repository = repository;
        this.ledgerService = ledgerService;
        this.fraudScorer = fraudScorer;
        this.auditEventPublisher = auditEventPublisher;
    }

    public SettlementResult execute(PaymentInstruction payment) {
        auditEventPublisher.paymentInitiated(payment);

        if (repository.hasTransaction(payment.endToEndId())) {
            LedgerResult duplicate = LedgerResult.duplicate(payment.endToEndId(), repository.entriesForTransaction(payment.endToEndId()));
            return new SettlementResult(payment.endToEndId(), SettlementStatus.DUPLICATE, null, duplicate, auditEventPublisher.events());
        }

        repository.assertCanDebit(payment.debtorAccount(), payment.amount(), payment.currency());

        FraudScore fraudScore = fraudScorer.score(payment);
        auditEventPublisher.fraudScored(payment, fraudScore);
        if (fraudScore.declined()) {
            auditEventPublisher.settlementDeclined(payment, fraudScore);
            return new SettlementResult(payment.endToEndId(), SettlementStatus.DECLINED, fraudScore, null, auditEventPublisher.events());
        }

        LedgerResult ledgerResult = ledgerService.postTransaction(payment);
        if (ledgerResult.status() == LedgerStatus.DUPLICATE) {
            return new SettlementResult(payment.endToEndId(), SettlementStatus.DUPLICATE, fraudScore, ledgerResult, auditEventPublisher.events());
        }
        SettlementStatus status = SettlementStatus.SETTLED;
        SettlementResult result = new SettlementResult(payment.endToEndId(), status, fraudScore, ledgerResult, auditEventPublisher.events());
        auditEventPublisher.settlementCompleted(result);
        return new SettlementResult(payment.endToEndId(), status, fraudScore, ledgerResult, auditEventPublisher.events());
    }
}
