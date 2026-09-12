const fs = require('fs');
let code = fs.readFileSync('frontend/src/App.tsx', 'utf8');

const emptyBlock = `          {activeTab !== 'Ledger Explorer' && (
            <motion.div
              key="empty"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="flex flex-col items-center justify-center h-[50vh] text-center"
            >
              <div className="w-24 h-24 mb-6 rounded-full liquid-glass flex items-center justify-center">
                <Wallet size={32} className="text-gray-500" />
              </div>
              <h2 className="text-2xl font-bold mb-2">{activeTab}</h2>
              <p className="text-gray-500 font-mono text-sm">Module is currently loading data vectors...</p>
            </motion.div>
          )}`;

const newBlock = `          {activeTab === 'Control Plane' && (
            <motion.div
              key="control-plane"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="space-y-12"
            >
              <div className="text-center mb-12">
                <h1 className="text-4xl md:text-5xl font-bold tracking-tighter mb-4">Command Center</h1>
                <p className="text-gray-400 max-w-[65ch] mx-auto leading-relaxed">
                  Real-time telemetry and cluster administration terminal.
                </p>
              </div>
              <Terminal />
            </motion.div>
          )}

          {activeTab !== 'Ledger Explorer' && activeTab !== 'Control Plane' && (
            <motion.div
              key="empty"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="flex flex-col items-center justify-center h-[50vh] text-center"
            >
              <div className="w-24 h-24 mb-6 rounded-full liquid-glass flex items-center justify-center">
                <Wallet size={32} className="text-gray-500" />
              </div>
              <h2 className="text-2xl font-bold mb-2">{activeTab}</h2>
              <p className="text-gray-500 font-mono text-sm">Module is currently loading data vectors...</p>
            </motion.div>
          )}`;

code = code.replace(emptyBlock, newBlock);
fs.writeFileSync('frontend/src/App.tsx', code);
