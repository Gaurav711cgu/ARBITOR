const fs = require('fs');
let app = fs.readFileSync('frontend/src/App.tsx', 'utf8');

const oldEmpty = `{activeTab !== 'Ledger Explorer' && activeTab !== 'Control Plane' && (
            <motion.div
              key="empty"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="flex flex-col items-center justify-center h-[50vh] text-center"
              role="tabpanel"
              id={\`panel-\${activeTab.replace(/\\s+/g, '-').toLowerCase()}\`}
              aria-labelledby={\`tab-\${activeTab.replace(/\\s+/g, '-').toLowerCase()}\`}
            >
              <div className="w-24 h-24 mb-6 rounded-full liquid-glass flex items-center justify-center" aria-hidden="true">
                <Wallet size={32} className="text-gray-400" />
              </div>
              <h2 className="text-2xl font-bold mb-2 text-white">{activeTab}</h2>
              <p className="text-gray-400 font-mono text-sm">Module is currently loading data vectors...</p>
            </motion.div>
          )}`;

const newEmpty = `{activeTab === 'Overview' && (
            <motion.div
              key="overview"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              role="tabpanel"
              id="panel-overview"
              aria-labelledby="tab-overview"
            >
              <Overview />
            </motion.div>
          )}

          {activeTab === 'API Ref' && (
            <motion.div
              key="empty"
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
              <p className="text-gray-400 font-mono text-sm">Module is currently loading data vectors...</p>
            </motion.div>
          )}`;

app = app.replace(oldEmpty, newEmpty);
fs.writeFileSync('frontend/src/App.tsx', app);

// fix unused vars
let overview = fs.readFileSync('frontend/src/components/Overview.tsx', 'utf8');
overview = overview.replace(', IdentificationCard', '');
fs.writeFileSync('frontend/src/components/Overview.tsx', overview);

let ledger = fs.readFileSync('frontend/src/components/LedgerExplorer.tsx', 'utf8');
ledger = ledger.replace('const [stats, setStats]', 'const [stats]');
fs.writeFileSync('frontend/src/components/LedgerExplorer.tsx', ledger);

