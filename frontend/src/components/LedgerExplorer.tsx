import { useState, useEffect, useRef } from 'react';
import { motion } from 'framer-motion';
import { TerminalWindow, Database, Heartbeat, Lightning, Swap } from '@phosphor-icons/react';

const FALLBACK_STREAM = [
  { id: '0x892a...1a9f', type: 'pacs.008', amount: '1,250,000.00', currency: 'USD', status: 'SETTLED' },
  { id: '0x11ab...9f22', type: 'camt.053', amount: '45,200.50', currency: 'EUR', status: 'PENDING' },
  { id: '0x77fa...8b11', type: 'pacs.002', amount: '990,000.00', currency: 'GBP', status: 'SETTLED' },
  { id: '0x992b...3c4d', type: 'pacs.008', amount: '12,500.00', currency: 'USD', status: 'FAILED' },
  { id: '0x33cf...1e88', type: 'pacs.008', amount: '4,100,000.00', currency: 'JPY', status: 'SETTLED' },
];

export function LedgerExplorer() {
  const [transactions, setTransactions] = useState(FALLBACK_STREAM);
  const [isLive, setIsLive] = useState(false);
  const [stats] = useState({ volume: '$14,299,012.50', tps: '4,192', latency: '12' });
  const intervalRef = useRef<number | null>(null);

  useEffect(() => {
    if (isLive) {
      const fetchTransactions = async () => {
        try {
          const res = await fetch('/api/transactions', {
            headers: {
              'x-arbiter-api-key': 'local-screenshot-key-123456' // using a dummy for demo, normally would be env var
            }
          });
          if (res.ok) {
            const data = await res.json();
            if (data.length > 0) {
              setTransactions(data);
            }
          }
        } catch (e) {
          console.error('Telemetry stream offline', e);
        }
      };
      
      fetchTransactions(); // initial fetch
      intervalRef.current = window.setInterval(fetchTransactions, 2000);
    } else {
      if (intervalRef.current !== null) {
        window.clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    }

    return () => {
      if (intervalRef.current !== null) {
        window.clearInterval(intervalRef.current);
      }
    };
  }, [isLive]);

  return (
    <motion.div
      key="ledger-explorer"
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -10 }}
      className="space-y-8"
    >
      <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6">
        <div>
          <h1 className="text-4xl md:text-5xl font-bold tracking-tighter mb-4">Ledger Explorer</h1>
          <p className="text-gray-400 max-w-[50ch] leading-relaxed">
            Immutable double-entry journal and real-time state vectors. Monitor global financial states with cryptographic certainty.
          </p>
        </div>
        
        {/* Search Bar - Bento style */}
        <div className="liquid-glass p-1.5 rounded-2xl flex items-center w-full md:w-[400px]">
          <div className="pl-4 pr-2 text-gray-400" aria-hidden="true">
            <TerminalWindow weight="fill" />
          </div>
          <input 
            type="text" 
            placeholder="Search Tx ID, Account, or Payload..." 
            aria-label="Search transactions"
            className="bg-transparent border-none outline-none text-sm w-full py-3 font-mono text-gray-200 placeholder:text-gray-500"
          />
          <button 
            className="bg-amber-500 text-bg-void px-4 py-2 rounded-xl text-sm font-semibold hover:bg-amber-400 transition-colors focus:ring-2 focus:ring-amber-500 focus:ring-offset-2 focus:ring-offset-bg-void"
            aria-label="Submit search query"
          >
            Query
          </button>
        </div>
      </div>

      {/* Stats Bento Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="liquid-glass rounded-[2rem] p-8 flex flex-col justify-between h-[200px] relative overflow-hidden group">
          <div className="absolute top-0 right-0 p-8 opacity-20 group-hover:opacity-40 transition-opacity" aria-hidden="true">
            <Database size={64} weight="duotone" className="text-amber-500" />
          </div>
          <h2 className="font-mono text-xs text-gray-400 font-semibold tracking-wider">TOTAL VOLUME (24H)</h2>
          <div className="font-mono text-4xl font-bold tabular-nums text-white">{stats.volume}</div>
        </div>
        
        <div className="liquid-glass rounded-[2rem] p-8 flex flex-col justify-between h-[200px] relative overflow-hidden group">
          <div className="absolute top-0 right-0 p-8 opacity-20 group-hover:opacity-40 transition-opacity" aria-hidden="true">
            <Heartbeat size={64} weight="duotone" className="text-emerald-500" />
          </div>
          <h2 className="font-mono text-xs text-gray-400 font-semibold tracking-wider">TPS (PEAK)</h2>
          <div className="font-mono text-4xl font-bold tabular-nums text-emerald-400">{stats.tps}</div>
        </div>

        <div className="liquid-glass rounded-[2rem] p-8 flex flex-col justify-between h-[200px] relative overflow-hidden group">
          <div className="absolute top-0 right-0 p-8 opacity-20 group-hover:opacity-40 transition-opacity" aria-hidden="true">
            <Lightning size={64} weight="duotone" className="text-cyan-500" />
          </div>
          <h2 className="font-mono text-xs text-gray-400 font-semibold tracking-wider">AVG LATENCY</h2>
          <div className="font-mono text-4xl font-bold tabular-nums text-cyan-400">{stats.latency}<span className="text-xl text-cyan-400/50 ml-1">ms</span></div>
        </div>
      </div>

      {/* Data Table */}
      <div className="liquid-glass rounded-[2rem] overflow-hidden">
        <div className="px-8 py-6 border-b border-white/5 flex justify-between items-center">
          <h3 className="font-semibold text-white">Recent Transactions</h3>
          <button 
            onClick={() => setIsLive(!isLive)}
            className={`text-xs font-mono transition-colors flex items-center gap-2 focus:outline-none focus:ring-2 focus:ring-amber-500 rounded px-3 py-1.5 ${isLive ? 'bg-amber-500/10 text-amber-400 border border-amber-500/30' : 'text-gray-500 hover:text-gray-400'}`}
            aria-label="Toggle live stream"
            aria-pressed={isLive}
          >
            <Swap weight="bold" aria-hidden="true" className={isLive ? 'animate-pulse' : ''} /> {isLive ? 'LIVE STREAM: ON' : 'LIVE STREAM: OFF'}
          </button>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <caption className="sr-only">Recent Transactions List</caption>
            <thead>
              <tr>
                <th scope="col" className="px-8 py-4 font-mono text-xs font-semibold text-gray-400 border-b border-white/5">TX ID</th>
                <th scope="col" className="px-8 py-4 font-mono text-xs font-semibold text-gray-400 border-b border-white/5">TYPE</th>
                <th scope="col" className="px-8 py-4 font-mono text-xs font-semibold text-gray-400 border-b border-white/5 text-right">AMOUNT</th>
                <th scope="col" className="px-8 py-4 font-mono text-xs font-semibold text-gray-400 border-b border-white/5 text-right">STATUS</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {transactions.map((tx, idx) => (
                <motion.tr 
                  key={tx.id + idx}
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  whileHover={{ backgroundColor: 'rgba(255,255,255,0.03)' }}
                  className="group cursor-pointer transition-colors focus-within:bg-white/5"
                  tabIndex={0}
                >
                  <td className="px-8 py-6 font-mono text-sm text-gray-300 group-hover:text-amber-400 group-focus:text-amber-400 transition-colors">{tx.id.substring(0, 12)}...</td>
                  <td className="px-8 py-6 font-mono text-sm">
                    <span className="bg-white/5 px-2 py-1 rounded text-gray-300">{tx.type}</span>
                  </td>
                  <td className="px-8 py-6 font-mono text-sm tabular-nums text-right text-gray-200">
                    {tx.amount} <span className="text-gray-500 ml-1">{tx.currency}</span>
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
                      }`} aria-hidden="true" />
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
  );
}
