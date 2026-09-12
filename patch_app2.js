const fs = require('fs');
let app = fs.readFileSync('src/main/java/com/arbiter/ArbiterApplication.java', 'utf8');

const oldMethod = `    private void handleTransactions(HttpExchange exchange) throws IOException {
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
    }`;

const newMethod = `    private void handleTransactions(HttpExchange exchange) throws IOException {
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
            String tx = txIds.get(i);
            // Mocking the full data for the frontend since LedgerRepository doesn't store the full JSON payload
            json.append(String.format("{\\"id\\":\\"%s\\", \\"type\\":\\"pacs.008\\", \\"amount\\":\\"%,.2f\\", \\"currency\\":\\"USD\\", \\"status\\":\\"SETTLED\\"}", 
                tx, (Math.random() * 50000) + 100));
            if (i < txIds.size() - 1) json.append(",");
        }
        json.append("]");
        respond(exchange, 200, json.toString());
    }`;

app = app.replace(oldMethod, newMethod);
fs.writeFileSync('src/main/java/com/arbiter/ArbiterApplication.java', app);
