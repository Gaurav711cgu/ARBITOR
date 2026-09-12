package com.arbiter.ledger;

import com.arbiter.audit.AuditEventPublisher;
import com.arbiter.domain.EntryType;
import com.arbiter.domain.JournalEntry;
import com.arbiter.domain.LedgerResult;
import com.arbiter.domain.PaymentInstruction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class LedgerService {
    private final LedgerRepository repository;
    private final AuditEventPublisher auditEventPublisher;

    public LedgerService(LedgerRepository repository, AuditEventPublisher auditEventPublisher) {
        this.repository = repository;
        this.auditEventPublisher = auditEventPublisher;
    }

    public LedgerResult postTransaction(PaymentInstruction payment) {
        if (repository.hasTransaction(payment.endToEndId())) {
            return LedgerResult.duplicate(payment.endToEndId(), repository.entriesForTransaction(payment.endToEndId()));
        }

        repository.assertCanDebit(payment.debtorAccount(), payment.amount(), payment.currency());

        Instant now = Instant.now();
        JournalEntry debit = new JournalEntry(
                payment.debtorAccount(),
                payment.endToEndId(),
                EntryType.DEBIT,
                payment.amount().negate(),
                payment.currency(),
                BigDecimal.ZERO,
                payment.creditorAccount(),
                payment.purposeCode(),
                now
        );
        JournalEntry credit = new JournalEntry(
                payment.creditorAccount(),
                payment.endToEndId(),
                EntryType.CREDIT,
                payment.amount(),
                payment.currency(),
                BigDecimal.ZERO,
                payment.debtorAccount(),
                payment.purposeCode(),
                now
        );

        BigDecimal sum = debit.amount().add(credit.amount());
        if (sum.compareTo(BigDecimal.ZERO) != 0) {
            throw new LedgerInvariantViolationException("double-entry invariant violated: " + sum);
        }

        try {
            repository.postAtomically(payment.endToEndId(), debit, credit);
        } catch (DuplicateTransactionException ex) {
            return LedgerResult.duplicate(payment.endToEndId(), repository.entriesForTransaction(payment.endToEndId()));
        }
        List<JournalEntry> storedEntries = repository.entriesForTransaction(payment.endToEndId());
        storedEntries.forEach(auditEventPublisher::journalEntryPosted);
        return LedgerResult.posted(payment.endToEndId(), storedEntries);
    }
}
