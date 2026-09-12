package com.arbiter;

import com.arbiter.api.ApiSecurity;
import com.arbiter.api.ApiAuth;
import com.arbiter.api.Json;
import com.arbiter.api.Responses;
import com.arbiter.audit.AuditEventPublisher;
import com.arbiter.domain.Account;
import com.arbiter.domain.PaymentInstruction;
import com.arbiter.fraud.RuleBasedFraudScorer;
import com.arbiter.iso20022.Iso20022MessageParser;
import com.arbiter.ledger.FileLedgerRepository;
import com.arbiter.ledger.LedgerService;
import com.arbiter.ledger.ReconciliationService;
import com.arbiter.settlement.SettlementSaga;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;

public final class ArbiterApplication {
    private final FileLedgerRepository repository;
    private final AuditEventPublisher audit;
    private final ApiAuth auth;
    private final SettlementSaga settlementSaga;
    private final Iso20022MessageParser isoParser = new Iso20022MessageParser();
    private final ReconciliationService reconciliationService;

    public ArbiterApplication() {
        this(Path.of(System.getProperty("arbiter.data.dir", "data")));
    }

    public ArbiterApplication(Path dataDirectory) {
        this.auth = ApiAuth.fromEnvironment();
        this.repository = new FileLedgerRepository(dataDirectory);
        this.audit = new AuditEventPublisher(dataDirectory.resolve("audit.log"));
        this.settlementSaga = new SettlementSaga(
                repository,
                new LedgerService(repository, audit),
                new RuleBasedFraudScorer(repository),
                audit
        );
        this.reconciliationService = new ReconciliationService(repository);
    }

    public static void main(String[] args) throws IOException {
        new ArbiterApplication().start(8080);
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", exchange -> respond(exchange, 200, "{\"status\":\"UP\"}"));
        server.createContext("/config", exchange -> respond(exchange, 200, Responses.config()));
        server.createContext("/accounts", this::handleAccounts);
        server.createContext("/payments", this::handlePayments);
        server.createContext("/payments/iso20022", this::handleIsoPayment);
        server.createContext("/reconcile", this::handleReconcile);
        server.createContext("/audit", this::handleAudit);
        server.createContext("/benchmarks/latest", this::handleBenchmark);
        server.createContext("/metrics", this::handleMetrics);
        server.createContext("/", this::handleStatic);
        server.start();
        System.out.println("Arbiter listening on http://localhost:" + port);
    }

    private void handleAccounts(HttpExchange exchange) throws IOException {
        try {
            auth.require(exchange);
            if ("GET".equals(exchange.getRequestMethod())) {
                respond(exchange, 200, Responses.accounts(repository.allAccounts()));
                return;
            }
            if ("POST".equals(exchange.getRequestMethod())) {
                ApiSecurity.requireContentType(exchange, "application/json");
                Map<String, String> json = Json.parseFlatObject(body(exchange));
                Account account = new Account(
                        Json.required(json, "accountId"),
                        Json.required(json, "currency"),
                        Json.decimal(json, "openingBalance")
                );
                repository.createAccount(account);
                respond(exchange, 201, Responses.account(account));
                return;
            }
            respond(exchange, 405, "{\"error\":\"method not allowed\"}");
        } catch (Exception ex) {
            respondError(exchange, ex);
        }
    }

    private void handlePayments(HttpExchange exchange) throws IOException {
        try {
            auth.require(exchange);
            if (!"POST".equals(exchange.getRequestMethod()) || exchange.getRequestURI().getPath().endsWith("/iso20022")) {
                respond(exchange, 404, "{\"error\":\"not found\"}");
                return;
            }
            ApiSecurity.requireContentType(exchange, "application/json");
            Map<String, String> json = Json.parseFlatObject(body(exchange));
            
            String debtor = json.containsKey("debtorAccountId") ? json.get("debtorAccountId") : Json.required(json, "debtorAccount");
            String creditor = json.containsKey("creditorAccountId") ? json.get("creditorAccountId") : Json.required(json, "creditorAccount");
            
            PaymentInstruction payment = new PaymentInstruction(
                    Json.required(json, "endToEndId"),
                    debtor,
                    creditor,
                    Json.decimal(json, "amount"),
                    Json.required(json, "currency"),
                    json.getOrDefault("purposeCode", "OTHR"),
                    exchange.getRemoteAddress().getAddress().getHostAddress()
            );
            respond(exchange, 200, Responses.settlement(settlementSaga.execute(payment)));
        } catch (Exception ex) {
            respondError(exchange, ex);
        }
    }

    private void handleIsoPayment(HttpExchange exchange) throws IOException {
        try {
            auth.require(exchange);
            if (!"POST".equals(exchange.getRequestMethod())) {
                respond(exchange, 405, "{\"error\":\"method not allowed\"}");
                return;
            }
            ApiSecurity.requireContentType(exchange, "application/xml");
            PaymentInstruction payment = isoParser.parsePain001(body(exchange));
            respond(exchange, 200, Responses.settlement(settlementSaga.execute(payment)));
        } catch (Exception ex) {
            respondError(exchange, ex);
        }
    }

    private void handleReconcile(HttpExchange exchange) throws IOException {
        try {
            auth.require(exchange);
        } catch (Exception ex) {
            respondError(exchange, ex);
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }
        respond(exchange, 200, Responses.reconcile(reconciliationService.reconcile()));
    }

    private void handleAudit(HttpExchange exchange) throws IOException {
        try {
            auth.require(exchange);
        } catch (Exception ex) {
            respondError(exchange, ex);
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }
        respond(exchange, 200, Responses.audit(audit.events()));
    }

    private void handleBenchmark(HttpExchange exchange) throws IOException {
        try {
            auth.require(exchange);
        } catch (Exception ex) {
            respondError(exchange, ex);
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }
        respond(exchange, 200, Responses.benchmark(Path.of("benchmark-results", "benchmark-latest.json")));
    }

    private void handleMetrics(HttpExchange exchange) throws IOException {
        try {
            auth.require(exchange);
        } catch (Exception ex) {
            respondError(exchange, ex);
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }
        BigDecimal journalSum = reconciliationService.reconcile().globalJournalSum();
        respond(exchange, 200, Responses.metrics(Path.of("benchmark-results", "benchmark-latest.json"), journalSum));
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        String fileName = "/".equals(requestPath) ? "index.html" : requestPath.substring(1);
        Path webRoot = Path.of("web").toAbsolutePath().normalize();
        Path file = webRoot.resolve(fileName).normalize();
        if (!file.startsWith(webRoot) || !Files.exists(file) || Files.isDirectory(file)) {
            respond(exchange, 404, "{\"error\":\"not found\"}");
            return;
        }
        byte[] bytes = Files.readAllBytes(file);
        ApiSecurity.applyHeaders(exchange);
        exchange.getResponseHeaders().set("content-type", contentType(fileName));
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String contentType(String fileName) {
        if (fileName.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (fileName.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (fileName.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "text/html; charset=utf-8";
    }

    private static String body(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readNBytes(ApiSecurity.MAX_BODY_BYTES + 1);
        if (bytes.length > ApiSecurity.MAX_BODY_BYTES) {
            throw new IllegalArgumentException("request body too large");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ApiSecurity.applyHeaders(exchange);
        exchange.getResponseHeaders().set("content-type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void respondError(HttpExchange exchange, Exception ex) throws IOException {
        if (ex instanceof SecurityException) {
            respond(exchange, 401, Responses.clientError("Unauthorized", "valid API key required"));
            return;
        }
        respond(exchange, 400, Responses.clientError(ex.getClass().getSimpleName(), safeMessage(ex)));
    }

    private static String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "request failed";
        }
        return message.replaceAll("[\\r\\n\\t]", " ");
    }
}
