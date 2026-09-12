package com.arbiter.ledger;

public final class InsufficientFundsException extends LedgerException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

