import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ShieldCheck, Wallet } from '@phosphor-icons/react';
import { Terminal } from './components/Terminal';
import { LedgerExplorer } from './components/LedgerExplorer';
import { Overview } from './components/Overview';

const TABS = ['Ledger Explorer', 'Control Plane', 'Overview', 'API Ref'];

export default function App() {
  const [activeTab, setActiveTab] = useState(TABS[0]);

  return (
    <div className="min-h-screen bg-bg-void text-gray-100 font-sans selection:bg-amber-500/30 overflow-x-hidden">
      {/* Navigation AppShell */}
      <nav 
        className="fixed top-6 left-1/2 -translate-x-1/2 z-50 liquid-glass rounded-full px-2 py-2 flex items-center gap-2 shadow-2xl"
        aria-label="Main Navigation"
      >
        <div className="flex items-center gap-2 pr-4 pl-2 border-r border-white/10" aria-hidden="true">
          <div className="bg-amber-500 text-bg-void p-1.5 rounded-full">
            <ShieldCheck weight="fill" size={20} />
          </div>
          <span className="font-bold tracking-widest text-sm uppercase text-white">Arbiter</span>
        </div>
        
        <div className="flex items-center gap-1" role="tablist" aria-label="Sections">
          {TABS.map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              role="tab"
              aria-selected={activeTab === tab}
              aria-controls={`panel-${tab.replace(/\s+/g, '-').toLowerCase()}`}
              id={`tab-${tab.replace(/\s+/g, '-').toLowerCase()}`}
              className={`relative px-4 py-2 rounded-full text-sm font-medium transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-amber-500 ${
                activeTab === tab ? 'text-bg-void' : 'text-gray-400 hover:text-white'
              }`}
            >
              {activeTab === tab && (
                <motion.div
                  layoutId="active-nav-pill"
                  className="absolute inset-0 bg-amber-500 rounded-full"
                  transition={{ type: 'spring', stiffness: 400, damping: 30 }}
                  style={{ originY: "0px" }}
                />
              )}
              <span className="relative z-10">{tab}</span>
            </button>
          ))}
        </div>
      </nav>

      {/* Main Content Area */}
      <main className="pt-32 pb-24 px-6 max-w-7xl mx-auto">
        <AnimatePresence mode="wait">
          {activeTab === 'Ledger Explorer' && (
            <div 
              key="ledger"
              role="tabpanel"
              id="panel-ledger-explorer"
              aria-labelledby="tab-ledger-explorer"
            >
              <LedgerExplorer />
            </div>
          )}

          {activeTab === 'Control Plane' && (
            <motion.div
              key="control-plane"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="space-y-12"
              role="tabpanel"
              id="panel-control-plane"
              aria-labelledby="tab-control-plane"
            >
              <div className="text-center mb-12">
                <h1 className="text-4xl md:text-5xl font-bold tracking-tighter mb-4 text-white">Command Center</h1>
                <p className="text-gray-400 max-w-[65ch] mx-auto leading-relaxed">
                  Real-time telemetry and cluster administration terminal.
                </p>
              </div>
              <Terminal />
            </motion.div>
          )}

          {activeTab === 'Overview' && (
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
          )}
        </AnimatePresence>
      </main>
    </div>
  );
}
