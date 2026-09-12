import { useEffect, useState, useRef } from 'react';
import { motion } from 'framer-motion';

const COMMANDS = [
  { text: 'arbiter-cli connect --cluster core-nyc-1', type: 'cmd' },
  { text: 'Resolving cluster endpoints...', type: 'info' },
  { text: 'Authenticated via mutual TLS. Certificate valid.', type: 'success' },
  { text: 'arbiter-cli sync --ledger ALL', type: 'cmd' },
  { text: 'Initiating real-time state vector synchronization...', type: 'info' },
  { text: 'Syncing block #899120...', type: 'log' },
  { text: 'Syncing block #899121...', type: 'log' },
  { text: 'Syncing block #899122...', type: 'log' },
  { text: 'Warning: High latency detected on shard 4', type: 'warn' },
  { text: 'Compensating via shard 5 failover...', type: 'info' },
  { text: 'Ledger synchronization complete.', type: 'success' },
  { text: 'Listening for incoming telemetry...', type: 'info' },
];

export function Terminal() {
  const [lines, setLines] = useState<{text: string, type: string}[]>([]);
  const containerRef = useRef<HTMLDivElement>(null);
  const startedRef = useRef(false);

  useEffect(() => {
    // Prevent StrictMode double execution
    if (startedRef.current) return;
    startedRef.current = true;

    let i = 0;
    const interval = setInterval(() => {
      if (i < COMMANDS.length) {
        setLines(prev => [...prev, COMMANDS[i]]);
        i++;
      } else {
        clearInterval(interval);
      }
    }, 800);

    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [lines]);

  return (
    <motion.div 
      initial={{ opacity: 0, scale: 0.98 }}
      animate={{ opacity: 1, scale: 1 }}
      className="w-full max-w-4xl mx-auto liquid-glass rounded-xl overflow-hidden shadow-2xl"
      role="region"
      aria-label="Terminal Output"
    >
      {/* Terminal Header */}
      <div className="bg-white/5 border-b border-white/10 px-4 py-3 flex items-center gap-4" aria-hidden="true">
        <div className="flex gap-2">
          <div className="w-3 h-3 rounded-full bg-red-500/80"></div>
          <div className="w-3 h-3 rounded-full bg-amber-500/80"></div>
          <div className="w-3 h-3 rounded-full bg-emerald-500/80"></div>
        </div>
        <div className="font-mono text-xs text-gray-400 flex-1 text-center">root@arbiter-core:~</div>
      </div>
      
      {/* Terminal Body */}
      <div 
        ref={containerRef}
        className="p-6 h-[400px] overflow-y-auto font-mono text-sm space-y-2 scroll-smooth focus:outline-none"
        tabIndex={0}
        aria-live="polite"
      >
        {lines.map((line, idx) => (
          <motion.div 
            key={idx}
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.2 }}
            className={`flex gap-3 ${
              line.type === 'cmd' ? 'text-white' :
              line.type === 'success' ? 'text-emerald-400' :
              line.type === 'warn' ? 'text-amber-400' :
              line.type === 'info' ? 'text-cyan-400' :
              'text-gray-300'
            }`}
          >
            {line.type === 'cmd' && <span className="text-emerald-500" aria-hidden="true">➜</span>}
            <span className="flex-1">{line.text}</span>
          </motion.div>
        ))}
        <div className="flex gap-3 text-white">
          <span className="text-emerald-500" aria-hidden="true">➜</span>
          <span className="w-2 h-4 bg-gray-400 inline-block mt-1 animate-cursor-blink" aria-hidden="true" />
        </div>
      </div>
    </motion.div>
  );
}
