import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { Zap, MapPin, ShieldCheck, BatteryCharging, ArrowRight, Wallet, Star, CheckCircle2, Sparkles, Activity, Lock, Cpu } from 'lucide-react';

export const Home = () => {
  const { user } = useAuth();

  return (
    <div className="relative overflow-hidden bg-[#061224] text-slate-100">
      {/* Stripe Iconic Multi-Color Gradient Mesh Background */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[1400px] h-[650px] -z-10 pointer-events-none opacity-80 overflow-hidden">
        {/* Angled blur blobs */}
        <div className="absolute top-[-20%] left-[10%] w-[500px] h-[500px] rounded-full bg-gradient-to-tr from-[#635bff] to-[#7928ca] blur-[130px] opacity-70 animate-pulse" />
        <div className="absolute top-[0%] right-[15%] w-[450px] h-[450px] rounded-full bg-gradient-to-br from-[#00d4ff] via-[#00f2fe] to-[#4facfe] blur-[120px] opacity-60" />
        <div className="absolute top-[30%] left-[30%] w-[550px] h-[400px] rounded-full bg-gradient-to-r from-[#ff6b9d] via-[#ec4899] to-[#f43f5e] blur-[140px] opacity-40" />
      </div>

      {/* Hero Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-20 pb-28">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
          {/* Left Column: Headlines & CTA */}
          <div className="lg:col-span-7 text-left">
            {/* Live Pill Badge */}
            <div className="inline-flex items-center gap-2.5 px-4 py-2 rounded-full bg-white/[0.08] border border-white/20 text-white text-xs font-semibold mb-8 backdrop-blur-xl shadow-lg shadow-black/20 hover:bg-white/[0.12] transition-colors">
              <span className="flex h-2 w-2 rounded-full bg-[#00d4ff] animate-ping" />
              <span className="text-slate-200">PostGIS Spatial Engine</span>
              <span className="text-white/40">•</span>
              <span className="text-cyan-300 font-bold">Two-Layer Slot Locks Active</span>
              <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
            </div>

            {/* Main Title */}
            <h1 className="text-5xl sm:text-7xl font-black tracking-tight text-white leading-[1.08] mb-6">
              Financial and spatial infrastructure for the{' '}
              <span className="bg-gradient-to-r from-[#00d4ff] via-[#635bff] to-[#ec4899] bg-clip-text text-transparent">
                Electric Vehicle Grid.
              </span>
            </h1>

            {/* Subtitle */}
            <p className="text-lg sm:text-xl text-slate-300 max-w-xl mb-10 leading-relaxed font-normal">
              ChargeConnect is a decentralized peer-to-peer EV marketplace. Drivers discover nearby fast chargers with zero
              queues. Hosts monetize private chargers and earn 90% net revenue with instant bank settlements.
            </p>

            {/* CTA Buttons (Stripe Style) */}
            <div className="flex flex-col sm:flex-row items-center gap-4">
              <Link
                to={user ? '/driver/find' : '/register?role=DRIVER'}
                className="w-full sm:w-auto px-8 py-4 bg-[#635bff] hover:bg-[#5340ff] text-white font-bold text-base rounded-full shadow-xl shadow-[#635bff]/40 hover:shadow-[#635bff]/60 hover:scale-[1.03] active:scale-[0.98] transition-all flex items-center justify-center gap-2.5"
              >
                <Zap className="w-5 h-5 fill-current" />
                Find Nearby Chargers
                <ArrowRight className="w-4 h-4 ml-1" />
              </Link>
              <Link
                to={user ? '/host/dashboard' : '/register?role=HOST'}
                className="w-full sm:w-auto px-8 py-4 bg-white/[0.07] hover:bg-white/[0.12] text-white font-semibold text-base rounded-full border border-white/20 hover:border-white/30 backdrop-blur-xl transition-all shadow-lg flex items-center justify-center gap-2"
              >
                Become a Host
              </Link>
            </div>
          </div>

          {/* Right Column: Floating Interactive Mockups (Stripe Hero Experience) */}
          <div className="lg:col-span-5 relative">
            <div className="relative mx-auto max-w-sm sm:max-w-md space-y-4">
              {/* Card 1: Live Charging Telemetry Widget */}
              <div className="p-6 rounded-3xl bg-[#091a33]/90 backdrop-blur-2xl border border-white/[0.12] shadow-2xl shadow-[#635bff]/20 transform hover:-translate-y-1 transition-transform">
                <div className="flex justify-between items-start mb-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-emerald-400 to-teal-400 flex items-center justify-center text-slate-950 font-bold shadow-md shadow-emerald-500/20">
                      <Zap className="w-5 h-5 fill-current" />
                    </div>
                    <div>
                      <h4 className="font-bold text-white text-sm">MG Road Hub #2</h4>
                      <p className="text-xs text-slate-400">CCS2 • 22.0 kW Fast AC</p>
                    </div>
                  </div>
                  <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center gap-1">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                    CHARGING
                  </span>
                </div>

                {/* Battery Gauge Progress */}
                <div className="mb-4">
                  <div className="flex justify-between text-xs font-semibold mb-1.5">
                    <span className="text-slate-300">Battery State</span>
                    <span className="text-emerald-400 font-bold">68% (+24 kWh)</span>
                  </div>
                  <div className="w-full h-3 bg-slate-800 rounded-full overflow-hidden p-0.5 border border-white/10">
                    <div className="h-full bg-gradient-to-r from-emerald-400 via-teal-300 to-cyan-400 rounded-full w-[68%] transition-all duration-500" />
                  </div>
                </div>

                {/* Telemetry Stats */}
                <div className="grid grid-cols-3 gap-2 pt-3 border-t border-white/[0.08] text-center">
                  <div>
                    <span className="text-[10px] uppercase font-bold text-slate-400">Power</span>
                    <p className="text-sm font-extrabold text-white mt-0.5">21.8 kW</p>
                  </div>
                  <div>
                    <span className="text-[10px] uppercase font-bold text-slate-400">Delivered</span>
                    <p className="text-sm font-extrabold text-cyan-300 mt-0.5">14.2 kWh</p>
                  </div>
                  <div>
                    <span className="text-[10px] uppercase font-bold text-slate-400">Live Bill</span>
                    <p className="text-sm font-extrabold text-emerald-400 mt-0.5">₹213.00</p>
                  </div>
                </div>
              </div>

              {/* Card 2: Dynamic Ledger Instant Payout Receipt */}
              <div className="p-5 rounded-2xl bg-[#0d2345]/90 backdrop-blur-2xl border border-white/[0.12] shadow-xl flex items-center justify-between transform hover:-translate-y-1 transition-transform">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-purple-500/20 text-purple-300 flex items-center justify-center border border-purple-500/30">
                    <Wallet className="w-4 h-4" />
                  </div>
                  <div>
                    <p className="text-xs font-bold text-white">Host Payout Disbursed</p>
                    <p className="text-[11px] text-slate-400">To Kiran Host • UPI</p>
                  </div>
                </div>
                <div className="text-right">
                  <span className="text-sm font-black text-purple-300">+₹486.00</span>
                  <p className="text-[10px] font-bold text-emerald-400 uppercase tracking-wider">PROCESSED</p>
                </div>
              </div>

              {/* Card 3: Two-Layer Locking Security Badge */}
              <div className="p-4 rounded-2xl bg-white/[0.04] backdrop-blur-xl border border-white/[0.08] flex items-center gap-3 text-xs text-slate-300">
                <Lock className="w-4 h-4 text-[#00d4ff] shrink-0" />
                <span>
                  <strong className="text-white">Redis 10-Min Hold:</strong> Guaranteed zero race conditions or double-bookings.
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Feature Section (Stripe Modern Cards) */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24 border-t border-white/[0.08]">
        <div className="max-w-3xl mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#635bff]/20 text-[#a59bff] border border-[#635bff]/30 text-xs font-bold uppercase tracking-wider mb-3">
            Core Architecture
          </div>
          <h2 className="text-4xl sm:text-5xl font-black text-white tracking-tight leading-tight">
            Designed for high concurrency, spatial accuracy, and financial auditability.
          </h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Feature 1 */}
          <div className="p-8 rounded-3xl bg-[#091a33]/60 backdrop-blur-xl border border-white/[0.08] hover:border-[#635bff]/50 hover:bg-[#091a33]/90 transition-all group shadow-xl">
            <div className="w-12 h-12 rounded-2xl bg-[#635bff]/20 text-[#a59bff] flex items-center justify-center mb-6 group-hover:scale-110 transition-transform border border-[#635bff]/30">
              <MapPin className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">PostGIS Spatial Discovery</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Curved-Earth ellipsoidal coordinate mapping using <code className="text-cyan-300">GEOGRAPHY(Point, 4326)</code>.
              Nearest-first indexing delivers search results in under 2 milliseconds across 100,000+ stations.
            </p>
          </div>

          {/* Feature 2 */}
          <div className="p-8 rounded-3xl bg-[#091a33]/60 backdrop-blur-xl border border-white/[0.08] hover:border-[#00d4ff]/50 hover:bg-[#091a33]/90 transition-all group shadow-xl">
            <div className="w-12 h-12 rounded-2xl bg-[#00d4ff]/20 text-[#00d4ff] flex items-center justify-center mb-6 group-hover:scale-110 transition-transform border border-[#00d4ff]/30">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">Two-Layer Slot Locking</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Eliminate double-booking conflicts completely. Layer 1 uses distributed Redis locks with a 10-minute hold TTL.
              Layer 2 enforces PostgreSQL transactional overlap constraints.
            </p>
          </div>

          {/* Feature 3 */}
          <div className="p-8 rounded-3xl bg-[#091a33]/60 backdrop-blur-xl border border-white/[0.08] hover:border-emerald-500/50 hover:bg-[#091a33]/90 transition-all group shadow-xl">
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform border border-emerald-500/30">
              <BatteryCharging className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">Live Charging Telemetry</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Real-time start and cutoff with hardware QR scanning. Calculates exact delivered kWh and live rupee cost,
              automatically updating booking status upon completion.
            </p>
          </div>

          {/* Feature 4 */}
          <div className="p-8 rounded-3xl bg-[#091a33]/60 backdrop-blur-xl border border-white/[0.08] hover:border-purple-500/50 hover:bg-[#091a33]/90 transition-all group shadow-xl">
            <div className="w-12 h-12 rounded-2xl bg-purple-500/20 text-purple-300 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform border border-purple-500/30">
              <Wallet className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">Dynamic Accounting Ledger</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Tamper-proof accounting with zero mutable balance columns. Host earnings are dynamically aggregated from
              completed sessions, deducting 10% platform fee and pending payouts.
            </p>
          </div>

          {/* Feature 5 */}
          <div className="p-8 rounded-3xl bg-[#091a33]/60 backdrop-blur-xl border border-white/[0.08] hover:border-amber-500/50 hover:bg-[#091a33]/90 transition-all group shadow-xl">
            <div className="w-12 h-12 rounded-2xl bg-amber-500/20 text-amber-300 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform border border-amber-500/30">
              <Star className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">Verified Reviews Only</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Enforces 100% review authenticity. Drivers can only review a charger after physically completing a charging
              session, dynamically recalculating charger average ratings.
            </p>
          </div>

          {/* Feature 6 */}
          <div className="p-8 rounded-3xl bg-[#091a33]/60 backdrop-blur-xl border border-white/[0.08] hover:border-cyan-500/50 hover:bg-[#091a33]/90 transition-all group shadow-xl">
            <div className="w-12 h-12 rounded-2xl bg-cyan-500/20 text-cyan-300 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform border border-cyan-500/30">
              <Activity className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">Executive Admin Analytics</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Real-time platform CEO dashboard: inspect charger verification queues, approve or reject hardware, and disburse
              host payouts with permanent bank UTR audit references.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
};
