package com.arbiter.domain;

import java.math.BigDecimal;

public record PaymentInstruction(
        String endToEndId,
        String debtorAccount,
        String creditorAccount,
        BigDecimal amount,
        String currency,
        String purposeCode,
        String sourceIp
) {
    public PaymentInstruction {
        endToEndId = Validation.endToEndId(endToEndId);
        debtorAccount = Validation.accountId(debtorAccount, "debtorAccount");
        creditorAccount = Validation.accountId(creditorAccount, "creditorAccount");
        currency = Validation.currency(currency);
        purposeCode = Validation.purposeCode(purposeCode);
        sourceIp = Validation.source(sourceIp);
        if (debtorAccount.equals(creditorAccount)) {
            throw new IllegalArgumentException("debtor and creditor must be different accounts");
        }
        amount = Validation.positivePaymentAmount(amount);
    }
}
