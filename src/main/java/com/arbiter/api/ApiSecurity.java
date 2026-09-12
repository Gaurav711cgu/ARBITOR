package com.arbiter.api;

import com.sun.net.httpserver.HttpExchange;

public final class ApiSecurity {
    public static final int MAX_BODY_BYTES = 64 * 1024;

    private ApiSecurity() {
    }

    public static void applyHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("x-content-type-options", "nosniff");
        exchange.getResponseHeaders().set("x-frame-options", "DENY");
        exchange.getResponseHeaders().set("referrer-policy", "no-referrer");
        exchange.getResponseHeaders().set("cache-control", "no-store");
        exchange.getResponseHeaders().set("content-security-policy", "default-src 'self'; img-src 'self'; script-src 'self'; style-src 'self'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'");
    }

    public static void requireContentType(HttpExchange exchange, String expected) {
        String contentType = exchange.getRequestHeaders().getFirst("content-type");
        if (contentType == null || !contentType.toLowerCase(java.util.Locale.ROOT).startsWith(expected)) {
            throw new IllegalArgumentException("content-type must be " + expected);
        }
    }
}
