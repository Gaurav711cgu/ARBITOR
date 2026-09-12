import { useState } from 'react';
import { motion } from 'framer-motion';
import { Bank, ShieldCheck, ArrowRight, SpinnerGap } from '@phosphor-icons/react';

export function Overview() {
  const [linkState, setLinkState] = useState<'idle' | 'linking' | 'exchanging' | 'connected'>('idle');

  const handleConnect = () => {
    setLinkState('linking');
    setTimeout(() => setLinkState('exchanging'), 1500);
    setTimeout(() => setLinkState('connected'), 3500);
  };

  return (
    <div className="space-y-12">
      <div className="text-center mb-12">
        <h1 className="text-4xl md:text-5xl font-bold tracking-tighter mb-4 text-white">Fiat Gateway</h1>
        <p className="text-gray-400 max-w-[65ch] mx-auto leading-relaxed">
          Secure ACH and wire funding rails via Plaid Link. Ensure KYC/AML compliance before bridging fiat into the Arbiter cryptographic ledger.
        </p>
      </div>

      <div className="max-w-2xl mx-auto">
        <div className="liquid-glass rounded-[2rem] p-8 md:p-12 relative overflow-hidden">
          {/* Decorative background blur */}
          <div className="absolute -top-32 -right-32 w-64 h-64 bg-amber-500/10 rounded-full blur-[100px]" />
          
          <div className="relative z-10 flex flex-col items-center text-center">
            {linkState === 'idle' && (
              <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex flex-col items-center">
                <div className="w-20 h-20 bg-white/5 rounded-full flex items-center justify-center mb-6">
                  <Bank size={32} weight="duotone" className="text-amber-500" />
                </div>
                <h2 className="text-2xl font-bold text-white mb-2">Connect Funding Source</h2>
                <p className="text-gray-400 mb-8 max-w-sm">
                  Link an external bank account to securely pull initial capital into your high-frequency ledger.
                </p>
                <button
                  onClick={handleConnect}
                  className="bg-white text-bg-void px-8 py-3 rounded-full font-bold hover:bg-gray-200 transition-colors flex items-center gap-2"
                >
                  Link Account via Plaid <ArrowRight weight="bold" />
                </button>
                
                <div className="mt-8 flex items-center gap-4 text-xs font-mono text-gray-500">
                  <span className="flex items-center gap-1.5"><ShieldCheck size={16} /> 256-bit AES</span>
                  <span className="w-1 h-1 bg-gray-700 rounded-full" />
                  <span>SOC 2 Type II</span>
                </div>
              </motion.div>
            )}

            {(linkState === 'linking' || linkState === 'exchanging') && (
              <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex flex-col items-center py-12">
                <SpinnerGap size={48} className="text-amber-500 animate-spin mb-6" />
                <h2 className="text-xl font-bold text-white mb-2 font-mono">
                  {linkState === 'linking' ? 'INITIATING PLAID LINK...' : 'EXCHANGING PUBLIC TOKEN...'}
                </h2>
                <p className="text-gray-400 font-mono text-sm max-w-sm">
                  {linkState === 'linking' 
                    ? 'Requesting short-lived link_token from API.'
                    : 'Upgrading to permanent access_token and fetching ACH routing details.'}
                </p>
              </motion.div>
            )}

            {linkState === 'connected' && (
              <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="w-full">
                <div className="flex items-center justify-center mb-8">
                  <div className="bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 px-4 py-1.5 rounded-full font-mono text-xs flex items-center gap-2">
                    <div className="w-2 h-2 bg-emerald-400 rounded-full animate-pulse" />
                    CONNECTION ESTABLISHED
                  </div>
                </div>
                
                <div className="bg-bg-void/50 border border-white/10 rounded-2xl p-6 text-left w-full space-y-4">
                  <div className="flex items-center gap-4 border-b border-white/5 pb-4">
                    <div className="w-12 h-12 bg-white/10 rounded-xl flex items-center justify-center">
                      <Bank size={24} weight="fill" className="text-gray-300" />
                    </div>
                    <div>
                      <h3 className="text-white font-bold">JPMorgan Chase</h3>
                      <p className="text-gray-400 font-mono text-sm">Plaid Item ID: itm_7x9Qk...</p>
                    </div>
                  </div>
                  
                  <div className="grid grid-cols-2 gap-4">
                    <div className="bg-white/5 rounded-xl p-4">
                      <p className="text-gray-500 font-mono text-xs mb-1">AVAILABLE BALANCE</p>
                      <p className="text-white font-mono text-xl tabular-nums">$2,540,119.50</p>
                    </div>
                    <div className="bg-white/5 rounded-xl p-4">
                      <p className="text-gray-500 font-mono text-xs mb-1">ACH ROUTING</p>
                      <p className="text-gray-300 font-mono text-sm tabular-nums">021000021</p>
                    </div>
                  </div>

                  <button className="w-full bg-amber-500 text-bg-void py-3 rounded-xl font-bold hover:bg-amber-400 transition-colors mt-4">
                    Initiate Wire Transfer
                  </button>
                </div>
              </motion.div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
