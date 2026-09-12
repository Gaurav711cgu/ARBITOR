package com.arbiter.api;

import com.sun.net.httpserver.HttpExchange;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ApiAuth {
    public static final String API_KEY_HEADER = "x-arbiter-api-key";

    private final byte[] expectedKey;

    private ApiAuth(String apiKey) {
        if (apiKey == null || apiKey.length() < 24) {
            throw new IllegalStateException("ARBITER_API_KEY or -Darbiter.api.key must be at least 24 characters");
        }
        this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    public static ApiAuth fromKey(String apiKey) {
        return new ApiAuth(apiKey);
    }

    public static ApiAuth fromEnvironment() {
        String property = System.getProperty("arbiter.api.key");
        String env = System.getenv("ARBITER_API_KEY");
        String key = property == null || property.isBlank() ? env : property;
        return new ApiAuth(key);
    }

    public void require(HttpExchange exchange) {
        String provided = exchange.getRequestHeaders().getFirst(API_KEY_HEADER);
        if (provided == null) {
            provided = exchange.getRequestHeaders().getFirst("X-API-Key");
        }
        if (!accepts(provided)) {
            throw new SecurityException("unauthorized");
        }
    }

    public boolean accepts(String provided) {
        return provided != null && constantTimeEquals(expectedKey, provided.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean constantTimeEquals(byte[] expected, byte[] actual) {
        return MessageDigest.isEqual(expected, actual);
    }
}
