const fs = require('fs');
let app = fs.readFileSync('src/main/java/com/arbiter/ArbiterApplication.java', 'utf8');

if (!app.includes('/transactions')) {
    const imports = 'import java.util.List;\nimport java.util.ArrayList;\nimport java.util.Collections;\nimport com.arbiter.ledger.LedgerRepository;\nimport com.arbiter.domain.JournalEntry;\n';
    app = app.replace('import com.arbiter.api.ApiAuth;', imports + 'import com.arbiter.api.ApiAuth;');

    // We need LedgerRepository in ArbiterApplication to query transactions.
    const field = 'private final AuditEventPublisher audit;\n    private final LedgerRepository ledgerRepository;';
    app = app.replace('private final AuditEventPublisher audit;', field);

    const constructorParams = 'ReconciliationService reconciliationService, AuditEventPublisher audit, LedgerRepository ledgerRepository';
    app = app.replace('ReconciliationService reconciliationService, AuditEventPublisher audit', constructorParams);
    
    const constructorAssign = 'this.audit = audit;\n        this.ledgerRepository = ledgerRepository;';
    app = app.replace('this.audit = audit;', constructorAssign);

    const initParams = 'new ReconciliationService(ledgerRepo), audit, ledgerRepo';
    app = app.replace('new ReconciliationService(ledgerRepo), audit', initParams);

    const contextMap = 'server.createContext("/audit", exchange -> app.handleAudit(exchange));\n        server.createContext("/transactions", exchange -> app.handleTransactions(exchange));';
    app = app.replace('server.createContext("/audit", exchange -> app.handleAudit(exchange));', contextMap);

    const method = `
    private void handleTransactions(HttpExchange exchange) throws IOException {
        try {
            auth.require(exchange);
        } catch (Exception ex) {
            respondError(exchange, ex);
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "{\\"error\\":\\"method not allowed\\"}");
            return;
        }
        List<String> txIds = ledgerRepository.getRecentTransactions();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < txIds.size(); i++) {
            json.append("\\"").append(txIds.get(i)).append("\\"");
            if (i < txIds.size() - 1) json.append(",");
        }
        json.append("]");
        respond(exchange, 200, json.toString());
    }
`;
    app = app.replace('private void handleMetrics(HttpExchange exchange) throws IOException {', method + '\n    private void handleMetrics(HttpExchange exchange) throws IOException {');
    fs.writeFileSync('src/main/java/com/arbiter/ArbiterApplication.java', app);
}
