package com.arbiter.api;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Json {
    private Json() {
    }

    public static Map<String, String> parseFlatObject(String json) {
        Parser parser = new Parser(json);
        return parser.parseObject();
    }

    public static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    public static BigDecimal decimal(Map<String, String> values, String key) {
        try {
            return new BigDecimal(required(values, key));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " must be a decimal number");
        }
    }

    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        escaped.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        escaped.append('"');
        return escaped.toString();
    }

    private static final class Parser {
        private final String input;
        private int index;

        Parser(String input) {
            if (input == null) {
                throw new IllegalArgumentException("JSON body is required");
            }
            this.input = input;
        }

        Map<String, String> parseObject() {
            skipWhitespace();
            expect('{');
            Map<String, String> values = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                ensureEnd();
                return values;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                values.put(key, parseScalarAsString());
                skipWhitespace();
                if (peek(',')) {
                    index++;
                    continue;
                }
                expect('}');
                ensureEnd();
                return values;
            }
        }

        private String parseScalarAsString() {
            if (peek('"')) {
                return parseString();
            }
            if (startsWith("true")) {
                index += 4;
                return "true";
            }
            if (startsWith("false")) {
                index += 5;
                return "false";
            }
            if (startsWith("null")) {
                index += 4;
                return "";
            }
            if (peek('{') || peek('[')) {
                throw new IllegalArgumentException("nested JSON values are not accepted by this endpoint");
            }
            return parseNumber();
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (index < input.length()) {
                char ch = input.charAt(index++);
                if (ch == '"') {
                    return value.toString();
                }
                if (ch == '\\') {
                    value.append(parseEscape());
                } else {
                    if (ch < 0x20) {
                        throw new IllegalArgumentException("control character in JSON string");
                    }
                    value.append(ch);
                }
            }
            throw new IllegalArgumentException("unterminated JSON string");
        }

        private char parseEscape() {
            if (index >= input.length()) {
                throw new IllegalArgumentException("unterminated JSON escape");
            }
            char escape = input.charAt(index++);
            return switch (escape) {
                case '"' -> '"';
                case '\\' -> '\\';
                case '/' -> '/';
                case 'b' -> '\b';
                case 'f' -> '\f';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 'u' -> parseUnicodeEscape();
                default -> throw new IllegalArgumentException("invalid JSON escape");
            };
        }

        private char parseUnicodeEscape() {
            if (index + 4 > input.length()) {
                throw new IllegalArgumentException("invalid unicode escape");
            }
            String hex = input.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("invalid unicode escape");
            }
        }

        private String parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            consumeDigits();
            if (peek('.')) {
                index++;
                consumeDigits();
            }
            if (peek('e') || peek('E')) {
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                consumeDigits();
            }
            if (start == index) {
                throw new IllegalArgumentException("invalid JSON value");
            }
            return input.substring(start, index);
        }

        private void consumeDigits() {
            int start = index;
            while (index < input.length() && Character.isDigit(input.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw new IllegalArgumentException("invalid JSON number");
            }
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private boolean peek(char expected) {
            return index < input.length() && input.charAt(index) == expected;
        }

        private boolean startsWith(String token) {
            return input.startsWith(token, index);
        }

        private void expect(char expected) {
            if (!peek(expected)) {
                throw new IllegalArgumentException("expected '" + expected + "'");
            }
            index++;
        }

        private void ensureEnd() {
            skipWhitespace();
            if (index != input.length()) {
                throw new IllegalArgumentException("unexpected trailing JSON content");
            }
        }
    }
}

