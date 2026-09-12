import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ShieldCheck, Heartbeat, TerminalWindow, Database, Lightning, Wallet, Swap } from '@phosphor-icons/react';

const navItems = ['Overview', 'Control Plane', 'Ledger Explorer', 'API Ref'];

// Fake data stream for perpetual motion
const DATA_STREAM = [
  { id: 'tx-8819', type: 'MINT', amount: '100,000.00', currency: 'USD', status: 'SETTLED' },
  { id: 'tx-2291', type: 'TRANSFER', amount: '5,420.11', currency: 'EUR', status: 'SETTLED' },
  { id: 'tx-7712', type: 'BURN', amount: '1,000.00', currency: 'GBP', status: 'PENDING' },
  { id: 'tx-0012', type: 'SWAP', amount: '84,119.50', currency: 'USD', status: 'FAILED' },
];

export default function App() {
  const [activeTab, setActiveTab] = useState('Ledger Explorer');

  return (
    <div className="min-h-[100dvh] bg-bg-void text-gray-100 flex flex-col font-sans selection:bg-amber-500/30">
      {/* AppShell Nav */}
      <div className="p-6 md:p-8">
        <nav className="liquid-glass rounded-full px-6 py-4 flex items-center justify-between max-w-7xl mx-auto">
          <div className="flex items-center gap-3">
            <ShieldCheck weight="duotone" className="text-amber-400 text-2xl" />
            <span className="font-mono font-bold tracking-tight text-lg">
              <span className="text-amber-400">ARBITER</span>
              <span className="text-gray-500">.sys</span>
            </span>
          </div>
          
          <div className="hidden md:flex items-center gap-2">
            {navItems.map((item) => (
              <button
                key={item}
                onClick={() => setActiveTab(item)}
                className={`relative px-4 py-2 text-sm font-medium transition-colors ${
                  activeTab === item ? 'text-white' : 'text-gray-400 hover:text-gray-200'
                }`}
              >
                {item}
                {activeTab === item && (
                  <motion.div
                    layoutId="nav-pill"
                    className="absolute inset-0 bg-white/10 rounded-full"
                    transition={{ type: 'spring', stiffness: 200, damping: 20 }}
                  />
                )}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 bg-black/40 px-3 py-1.5 rounded-full border border-white/5">
              <motion.div 
                animate={{ opacity: [1, 0.4, 1] }} 
                transition={{ duration: 2, repeat: Infinity }}
                className="w-2 h-2 rounded-full bg-emerald-500" 
              />
              <span className="font-mono text-xs text-emerald-500 font-medium tracking-wide">SYNCED</span>
            </div>
          </div>
        </nav>
      </div>

      {/* Main Content */}
      <main className="flex-1 w-full max-w-7xl mx-auto px-6 md:px-8 pb-20">
        <AnimatePresence mode="wait">
          {activeTab === 'Ledger Explorer' && (
            <motion.div
              key="ledger"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.3 }}
              className="space-y-12"
            >
              {/* Header Section */}
              <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
                <div>
                  <h1 className="text-4xl md:text-5xl font-bold tracking-tighter mb-4">Ledger Explorer</h1>
                  <p className="text-gray-400 max-w-[65ch] leading-relaxed">
                    Immutable double-entry journal and real-time state vectors. Monitor global financial states with cryptographic certainty.
                  </p>
                </div>
                
                {/* Search Bar - Bento style */}
                <div className="liquid-glass p-1.5 rounded-2xl flex items-center w-full md:w-[400px]">
                  <div className="pl-4 pr-2 text-gray-500">
                    <TerminalWindow weight="fill" />
                  </div>
                  <input 
                    type="text" 
                    placeholder="Search Tx ID, Account, or Payload..." 
                    className="bg-transparent border-none outline-none text-sm w-full py-3 font-mono text-gray-200 placeholder:text-gray-600"
                  />
                  <button className="bg-amber-500 text-bg-void px-4 py-2 rounded-xl text-sm font-semibold hover:bg-amber-400 transition-colors">
                    Query
                  </button>
                </div>
              </div>

              {/* Stats Bento Grid */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="liquid-glass rounded-[2rem] p-8 flex flex-col justify-between h-[200px] relative overflow-hidden group">
                  <div className="absolute top-0 right-0 p-8 opacity-20 group-hover:opacity-40 transition-opacity">
                    <Database size={64} weight="duotone" className="text-amber-500" />
                  </div>
                  <span className="font-mono text-xs text-gray-500 font-semibold tracking-wider">TOTAL VOLUME (24H)</span>
                  <div className="font-mono text-4xl font-bold tabular-nums">$14,299,012.50</div>
                </div>
                
                <div className="liquid-glass rounded-[2rem] p-8 flex flex-col justify-between h-[200px] relative overflow-hidden group">
                  <div className="absolute top-0 right-0 p-8 opacity-20 group-hover:opacity-40 transition-opacity">
                    <Heartbeat size={64} weight="duotone" className="text-emerald-500" />
                  </div>
                  <span className="font-mono text-xs text-gray-500 font-semibold tracking-wider">TPS (PEAK)</span>
                  <div className="font-mono text-4xl font-bold tabular-nums text-emerald-400">4,192</div>
                </div>

                <div className="liquid-glass rounded-[2rem] p-8 flex flex-col justify-between h-[200px] relative overflow-hidden group">
                  <div className="absolute top-0 right-0 p-8 opacity-20 group-hover:opacity-40 transition-opacity">
                    <Lightning size={64} weight="duotone" className="text-cyan-500" />
                  </div>
                  <span className="font-mono text-xs text-gray-500 font-semibold tracking-wider">AVG LATENCY</span>
                  <div className="font-mono text-4xl font-bold tabular-nums text-cyan-400">12<span className="text-xl text-cyan-400/50">ms</span></div>
                </div>
              </div>

              {/* Data Table */}
              <div className="liquid-glass rounded-[2rem] overflow-hidden">
                <div className="px-8 py-6 border-b border-white/5 flex justify-between items-center">
                  <h3 className="font-semibold">Recent Transactions</h3>
                  <button className="text-xs font-mono text-amber-500 hover:text-amber-400 transition-colors flex items-center gap-2">
                    <Swap weight="bold" /> LIVE STREAM
                  </button>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse">
                    <thead>
                      <tr>
                        <th className="px-8 py-4 font-mono text-xs font-semibold text-gray-500 border-b border-white/5">TX ID</th>
                        <th className="px-8 py-4 font-mono text-xs font-semibold text-gray-500 border-b border-white/5">TYPE</th>
                        <th className="px-8 py-4 font-mono text-xs font-semibold text-gray-500 border-b border-white/5 text-right">AMOUNT</th>
                        <th className="px-8 py-4 font-mono text-xs font-semibold text-gray-500 border-b border-white/5 text-right">STATUS</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                      {DATA_STREAM.map((tx) => (
                        <motion.tr 
                          key={tx.id}
                          initial={{ opacity: 0, x: -10 }}
                          animate={{ opacity: 1, x: 0 }}
                          whileHover={{ backgroundColor: 'rgba(255,255,255,0.03)' }}
                          className="group cursor-pointer transition-colors"
                        >
                          <td className="px-8 py-6 font-mono text-sm text-gray-300 group-hover:text-amber-400 transition-colors">{tx.id}</td>
                          <td className="px-8 py-6 font-mono text-sm">
                            <span className="bg-white/5 px-2 py-1 rounded text-gray-300">{tx.type}</span>
                          </td>
                          <td className="px-8 py-6 font-mono text-sm tabular-nums text-right">
                            {tx.amount} <span className="text-gray-500">{tx.currency}</span>
                          </td>
                          <td className="px-8 py-6 text-right">
                            <span className={`inline-flex items-center gap-1.5 font-mono text-xs px-2.5 py-1 rounded-full border ${
                              tx.status === 'SETTLED' ? 'border-emerald-500/30 text-emerald-400 bg-emerald-500/10' :
                              tx.status === 'FAILED' ? 'border-red-500/30 text-red-400 bg-red-500/10' :
                              'border-amber-500/30 text-amber-400 bg-amber-500/10'
                            }`}>
                              <span className={`w-1.5 h-1.5 rounded-full ${
                                tx.status === 'SETTLED' ? 'bg-emerald-400' :
                                tx.status === 'FAILED' ? 'bg-red-400' :
                                'bg-amber-400 animate-pulse'
                              }`} />
                              {tx.status}
                            </span>
                          </td>
                        </motion.tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </motion.div>
          )}

          {activeTab === 'Control Plane' && (
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
          )}
        </AnimatePresence>
      </main>
    </div>
  );
}
