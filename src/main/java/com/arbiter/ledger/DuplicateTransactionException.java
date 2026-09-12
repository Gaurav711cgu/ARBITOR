package com.arbiter.ledger;

public final class DuplicateTransactionException extends LedgerException {
    public DuplicateTransactionException(String message) {
        super(message);
    }
}

