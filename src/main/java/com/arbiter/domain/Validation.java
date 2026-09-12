package com.arbiter.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

public final class Validation {
    private static final Pattern ACCOUNT_ID = Pattern.compile("[A-Za-z0-9:_-]{1,64}");
    private static final Pattern END_TO_END_ID = Pattern.compile("[A-Za-z0-9:._-]{1,128}");
    private static final Pattern CURRENCY = Pattern.compile("[A-Z]{3}");
    private static final Pattern PURPOSE_CODE = Pattern.compile("[A-Z0-9]{1,8}");
    private static final Pattern SOURCE = Pattern.compile("[A-Za-z0-9:._-]{1,128}");
    private static final BigDecimal MAX_PAYMENT_AMOUNT = new BigDecimal("1000000.00");

    private Validation() {
    }

    public static String accountId(String value, String field) {
        String normalized = requireText(value, field);
        if (!ACCOUNT_ID.matcher(normalized).matches()) {
            throw new IllegalArgumentException(field + " must be 1-64 chars: letters, numbers, colon, underscore, dash");
        }
        return normalized;
    }

    public static String endToEndId(String value) {
        String normalized = requireText(value, "endToEndId");
        if (!END_TO_END_ID.matcher(normalized).matches()) {
            throw new IllegalArgumentException("endToEndId must be 1-128 chars: letters, numbers, colon, dot, underscore, dash");
        }
        return normalized;
    }

    public static String currency(String value) {
        String normalized = requireText(value, "currency").toUpperCase();
        if (!CURRENCY.matcher(normalized).matches()) {
            throw new IllegalArgumentException("currency must be a 3-letter ISO code");
        }
        return normalized;
    }

    public static String purposeCode(String value) {
        String normalized = value == null || value.isBlank() ? "OTHR" : value.trim().toUpperCase();
        if (!PURPOSE_CODE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("purposeCode must be 1-8 uppercase letters or digits");
        }
        return normalized;
    }

    public static String source(String value) {
        String normalized = value == null || value.isBlank() ? "unknown" : value.trim();
        if (!SOURCE.matcher(normalized).matches()) {
            return "unknown";
        }
        return normalized;
    }

    public static BigDecimal money(BigDecimal value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(field + " must have exactly two decimal places");
        }
    }

    public static BigDecimal positivePaymentAmount(BigDecimal value) {
        BigDecimal money = money(value, "amount");
        if (money.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (money.compareTo(MAX_PAYMENT_AMOUNT) > 0) {
            throw new IllegalArgumentException("amount exceeds max payment limit");
        }
        return money;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}

