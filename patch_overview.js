const fs = require('fs');
let app = fs.readFileSync('frontend/src/App.tsx', 'utf8');

if (!app.includes('import { Overview }')) {
    app = app.replace("import { LedgerExplorer } from './components/LedgerExplorer';", "import { LedgerExplorer } from './components/LedgerExplorer';\nimport { Overview } from './components/Overview';");
    
    // Replace the empty state for Overview with the actual component
    const emptyStateRegex = /\{activeTab !== 'Ledger Explorer' && activeTab !== 'Control Plane' && \([\s\S]*?\}\)/;
    
    const newJSX = `{activeTab === 'Overview' && (
            <div 
              key="overview"
              role="tabpanel"
              id="panel-overview"
              aria-labelledby="tab-overview"
            >
              <Overview />
            </div>
          )}

          {activeTab === 'API Ref' && (
            <motion.div
              key="api-ref"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="flex flex-col items-center justify-center h-[50vh] text-center"
              role="tabpanel"
              id="panel-api-ref"
              aria-labelledby="tab-api-ref"
            >
              <div className="w-24 h-24 mb-6 rounded-full liquid-glass flex items-center justify-center" aria-hidden="true">
                <Wallet size={32} className="text-gray-400" />
              </div>
              <h2 className="text-2xl font-bold mb-2 text-white">API Reference</h2>
              <p className="text-gray-400 font-mono text-sm">Documentation portal generating...</p>
            </motion.div>
          )}`;
          
    app = app.replace(emptyStateRegex, newJSX);
    fs.writeFileSync('frontend/src/App.tsx', app);
}
