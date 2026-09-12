const fs = require('fs');

let ledgerRepo = fs.readFileSync('src/main/java/com/arbiter/ledger/LedgerRepository.java', 'utf8');
if (!ledgerRepo.includes('List<String> getRecentTransactions();')) {
    ledgerRepo = ledgerRepo.replace('Account account(String accountId);', 'Account account(String accountId);\n    java.util.List<String> getRecentTransactions();');
    fs.writeFileSync('src/main/java/com/arbiter/ledger/LedgerRepository.java', ledgerRepo);
}

let fileLedgerRepo = fs.readFileSync('src/main/java/com/arbiter/ledger/FileLedgerRepository.java', 'utf8');
if (!fileLedgerRepo.includes('public List<String> getRecentTransactions()')) {
    const importList = 'import java.util.List;\nimport java.util.ArrayList;\nimport java.util.Collections;\n';
    fileLedgerRepo = fileLedgerRepo.replace('import java.util.Map;', importList + 'import java.util.Map;');
    
    // We can just keep an ordered list of transaction IDs to easily get the recent ones.
    const fieldDecl = 'private final Set<String> processedTransactions = new HashSet<>();\n    private final List<String> orderedTransactions = new ArrayList<>();';
    fileLedgerRepo = fileLedgerRepo.replace('private final Set<String> processedTransactions = new HashSet<>();', fieldDecl);
    
    // add to orderedTransactions in applyTransactionRecord
    const processAdd = 'processedTransactions.add(transactionId);\n        orderedTransactions.add(transactionId);';
    fileLedgerRepo = fileLedgerRepo.replace('processedTransactions.add(transactionId);', processAdd);
    
    const replayAdd = 'processedTransactions.add(fields[1]);\n        orderedTransactions.add(fields[1]);';
    fileLedgerRepo = fileLedgerRepo.replace('processedTransactions.add(fields[1]);', replayAdd);
    
    // Add method implementation
    const methodImpl = `
    @Override
    public synchronized List<String> getRecentTransactions() {
        int start = Math.max(0, orderedTransactions.size() - 20);
        List<String> recent = new ArrayList<>(orderedTransactions.subList(start, orderedTransactions.size()));
        Collections.reverse(recent);
        return recent;
    }
`;
    fileLedgerRepo = fileLedgerRepo.replace('public synchronized void attemptOverwriteForTest', methodImpl + '\n    public synchronized void attemptOverwriteForTest');
    fs.writeFileSync('src/main/java/com/arbiter/ledger/FileLedgerRepository.java', fileLedgerRepo);
}
