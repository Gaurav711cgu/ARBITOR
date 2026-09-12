package com.arbiter.api;

import com.arbiter.domain.Account;
import com.arbiter.domain.FraudScore;
import com.arbiter.domain.JournalEntry;
import com.arbiter.domain.LedgerResult;
import com.arbiter.domain.SettlementResult;
import com.arbiter.ledger.ReconciliationReport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public final class Responses {
    private Responses() {
    }

    public static String account(Account account) {
        return "{"
                + "\"accountId\":" + Json.quote(account.accountId()) + ","
                + "\"currency\":" + Json.quote(account.currency()) + ","
                + "\"balance\":" + Json.quote(account.balance().toPlainString())
                + "}";
    }

    public static String accounts(List<Account> accounts) {
        return accounts.stream().map(Responses::account).collect(Collectors.joining(",", "[", "]"));
    }

    public static String settlement(SettlementResult result) {
        return "{"
                + "\"transactionId\":" + Json.quote(result.transactionId()) + ","
                + "\"status\":" + Json.quote(result.status().name()) + ","
                + "\"fraudScore\":" + fraud(result.fraudScore()) + ","
                + "\"ledger\":" + ledger(result.ledgerResult()) + ","
                + "\"auditEventCount\":" + result.auditEvents().size()
                + "}";
    }

    public static String entries(List<JournalEntry> entries) {
        return entries.stream().map(Responses::entry).collect(Collectors.joining(",", "[", "]"));
    }

    public static String reconcile(boolean balanced, BigDecimal total, int entryCount) {
        return "{"
                + "\"balanced\":" + balanced + ","
                + "\"globalJournalSum\":" + Json.quote(total.toPlainString()) + ","
                + "\"entryCount\":" + entryCount
                + "}";
    }

    public static String reconcile(ReconciliationReport report) {
        String issues = report.issues().stream()
                .map(issue -> "{"
                        + "\"accountId\":" + Json.quote(issue.accountId()) + ","
                        + "\"expectedBalance\":" + Json.quote(issue.expectedBalance().toPlainString()) + ","
                        + "\"actualBalance\":" + Json.quote(issue.actualBalance().toPlainString()) + ","
                        + "\"reason\":" + Json.quote(issue.reason())
                        + "}")
                .collect(Collectors.joining(",", "[", "]"));
        return "{"
                + "\"balanced\":" + report.balanced() + ","
                + "\"globalJournalSum\":" + Json.quote(report.globalJournalSum().toPlainString()) + ","
                + "\"entryCount\":" + report.entryCount() + ","
                + "\"accountCount\":" + report.accountCount() + ","
                + "\"issueCount\":" + report.issues().size() + ","
                + "\"issues\":" + issues
                + "}";
    }

    public static String error(Exception exception) {
        return "{"
                + "\"error\":" + Json.quote(exception.getClass().getSimpleName()) + ","
                + "\"message\":" + Json.quote(exception.getMessage())
                + "}";
    }

    public static String clientError(String code, String message) {
        return "{"
                + "\"error\":" + Json.quote(code) + ","
                + "\"message\":" + Json.quote(message)
                + "}";
    }

    public static String config() {
        return "{"
                + "\"authRequired\":true,"
                + "\"apiKeyHeader\":" + Json.quote(ApiAuth.API_KEY_HEADER)
                + "}";
    }

    public static String audit(List<String> events) {
        String lines = events.stream().map(Json::quote).collect(Collectors.joining(",", "[", "]"));
        return "{"
                + "\"eventCount\":" + events.size() + ","
                + "\"events\":" + lines
                + "}";
    }

    public static String benchmark(Path benchmarkJson) {
        try {
            if (!Files.exists(benchmarkJson)) {
                return "{\"available\":false,\"message\":\"run ./scripts/benchmark.sh to generate local benchmark evidence\"}";
            }
            String json = Files.readString(benchmarkJson).trim();
            return "{\"available\":true,\"result\":" + json + "}";
        } catch (Exception ex) {
            return "{\"available\":false,\"message\":\"benchmark result could not be read\"}";
        }
    }

    public static String metrics(Path benchmarkJson, BigDecimal journalSum) {
        try {
            if (!Files.exists(benchmarkJson)) {
                return "{\"totalRequests\":0,\"duplicateAttempts\":0,\"journalSum\":" + journalSum.toPlainString() + ",\"p99LatencyUs\":0,\"errors\":0}";
            }
            String json = Files.readString(benchmarkJson).trim();
            // Parse json to extract values (or just use regex hack since it's a known format)
            int settled = Integer.parseInt(json.replaceAll(".*\"settled\"\\s*:\\s*(\\d+).*", "$1"));
            int dup = Integer.parseInt(json.replaceAll(".*\"duplicateResponses\"\\s*:\\s*(\\d+).*", "$1"));
            int p99 = Integer.parseInt(json.replaceAll(".*\"p99Micros\"\\s*:\\s*(\\d+).*", "$1"));
            int err = Integer.parseInt(json.replaceAll(".*\"errors\"\\s*:\\s*(\\d+).*", "$1"));
            
            return "{"
                + "\"totalRequests\":" + settled + ","
                + "\"duplicateAttempts\":" + dup + ","
                + "\"journalSum\":" + journalSum.toPlainString() + ","
                + "\"p99LatencyUs\":" + p99 + ","
                + "\"errors\":" + err
                + "}";
        } catch (Exception ex) {
            return "{\"totalRequests\":0,\"duplicateAttempts\":0,\"journalSum\":" + journalSum.toPlainString() + ",\"p99LatencyUs\":0,\"errors\":0}";
        }
    }

    private static String fraud(FraudScore score) {
        if (score == null) {
            return "null";
        }
        String reasons = score.reasons().stream().map(Json::quote).collect(Collectors.joining(",", "[", "]"));
        return "{"
                + "\"value\":" + String.format(java.util.Locale.ROOT, "%.2f", score.value()) + ","
                + "\"decision\":" + Json.quote(score.decision().name()) + ","
                + "\"reasons\":" + reasons
                + "}";
    }

    private static String ledger(LedgerResult result) {
        if (result == null) {
            return "null";
        }
        return "{"
                + "\"status\":" + Json.quote(result.status().name()) + ","
                + "\"entries\":" + entries(result.entries())
                + "}";
    }

    private static String entry(JournalEntry entry) {
        return "{"
                + "\"accountId\":" + Json.quote(entry.accountId()) + ","
                + "\"transactionId\":" + Json.quote(entry.transactionId()) + ","
                + "\"entryType\":" + Json.quote(entry.entryType().name()) + ","
                + "\"amount\":" + Json.quote(entry.amount().toPlainString()) + ","
                + "\"currency\":" + Json.quote(entry.currency()) + ","
                + "\"balanceAfter\":" + Json.quote(entry.balanceAfter().toPlainString()) + ","
                + "\"counterpartyAccount\":" + Json.quote(entry.counterpartyAccount()) + ","
                + "\"purposeCode\":" + Json.quote(entry.purposeCode()) + ","
                + "\"timestamp\":" + Json.quote(entry.timestamp().toString())
                + "}";
    }
}
