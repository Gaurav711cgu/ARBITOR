package com.arbiter.domain;

import java.math.BigDecimal;
import java.time.Instant;

public final class Account {
    private final String accountId;
    private final String currency;
    private final BigDecimal openingBalance;
    private BigDecimal balance;
    private final Instant createdAt;

    public Account(String accountId, String currency, BigDecimal openingBalance) {
        this.accountId = Validation.accountId(accountId, "accountId");
        this.currency = Validation.currency(currency);
        this.openingBalance = Validation.money(openingBalance, "openingBalance");
        this.balance = this.openingBalance;
        this.createdAt = Instant.now();
    }

    public String accountId() {
        return accountId;
    }

    public String currency() {
        return currency;
    }

    public BigDecimal balance() {
        return balance;
    }

    public BigDecimal openingBalance() {
        return openingBalance;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public void apply(BigDecimal signedAmount) {
        balance = Validation.money(balance.add(signedAmount), "balance");
    }
}
