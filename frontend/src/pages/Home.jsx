import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { Zap, MapPin, ShieldCheck, BatteryCharging, ArrowRight, Wallet, Star, CheckCircle, Sparkles } from 'lucide-react';

export const Home = () => {
  const { user } = useAuth();

  return (
    <div className="relative overflow-hidden">
      {/* Background Ambient Glows (Stripe Style) */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[1000px] h-[500px] bg-gradient-to-b from-emerald-500/15 via-teal-500/5 to-transparent rounded-full blur-3xl pointer-events-none -z-10" />
      <div className="absolute top-80 right-0 w-[500px] h-[500px] bg-purple-500/10 rounded-full blur-3xl pointer-events-none -z-10" />

      {/* Hero Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-20 pb-24 text-center">
        {/* Live Status Pill */}
        <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-slate-900/90 border border-emerald-500/30 text-emerald-300 text-xs font-semibold mb-8 shadow-lg shadow-emerald-950/40 backdrop-blur-md">
          <span className="flex h-2 w-2 rounded-full bg-emerald-400 animate-ping" />
          <span>PostGIS Spatial Grid • Two-Layer Slot Locking Active</span>
        </div>

        {/* Main Headline */}
        <h1 className="text-4xl sm:text-6xl lg:text-7xl font-black tracking-tight text-white max-w-4xl mx-auto leading-[1.1] mb-6">
          The Decentralized Grid for{' '}
          <span className="bg-gradient-to-r from-emerald-400 via-teal-300 to-cyan-400 bg-clip-text text-transparent">
            Electric Vehicles
          </span>
        </h1>

        {/* Subtitle */}
        <p className="text-lg sm:text-xl text-slate-400 max-w-2xl mx-auto mb-10 leading-relaxed font-normal">
          Discover fast, verified EV charging stations near you or monetize your private home charger with guaranteed slot
          reservations and instant UPI payouts.
        </p>

        {/* CTA Buttons */}
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4 max-w-md mx-auto">
          <Link
            to={user ? '/driver/find' : '/register?role=DRIVER'}
            className="w-full sm:w-auto px-7 py-3.5 bg-gradient-to-r from-emerald-400 to-teal-400 hover:from-emerald-300 hover:to-teal-300 text-slate-950 font-bold text-base rounded-2xl shadow-xl shadow-emerald-500/20 hover:shadow-emerald-500/35 hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2"
          >
            <Zap className="w-5 h-5 fill-current" />
            Find Nearby Chargers
          </Link>
          <Link
            to={user ? '/host/dashboard' : '/register?role=HOST'}
            className="w-full sm:w-auto px-7 py-3.5 bg-slate-900/90 hover:bg-slate-800/90 text-white font-semibold text-base rounded-2xl border border-slate-700/80 hover:border-slate-600 transition-all shadow-lg flex items-center justify-center gap-2"
          >
            Become a Host
            <ArrowRight className="w-4 h-4 text-slate-400" />
          </Link>
        </div>

        {/* Quick Trust Badges */}
        <div className="mt-14 pt-10 border-t border-slate-800/60 max-w-3xl mx-auto grid grid-cols-2 sm:grid-cols-4 gap-4 text-slate-400 text-xs font-medium">
          <div className="flex items-center justify-center gap-2">
            <CheckCircle className="w-4 h-4 text-emerald-400" />
            <span>Zero Double-Booking</span>
          </div>
          <div className="flex items-center justify-center gap-2">
            <CheckCircle className="w-4 h-4 text-emerald-400" />
            <span>Verified Hardware</span>
          </div>
          <div className="flex items-center justify-center gap-2">
            <CheckCircle className="w-4 h-4 text-emerald-400" />
            <span>Razorpay Escrow</span>
          </div>
          <div className="flex items-center justify-center gap-2">
            <CheckCircle className="w-4 h-4 text-emerald-400" />
            <span>Dynamic Payouts</span>
          </div>
        </div>
      </section>

      {/* Feature Grid (Stripe Style) */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 border-t border-slate-800/80">
        <div className="text-center max-w-2xl mx-auto mb-16">
          <h2 className="text-xs font-bold uppercase tracking-widest text-emerald-400 mb-2">Engineered For Reliability</h2>
          <p className="text-3xl font-extrabold text-white">Everything needed to power peer-to-peer EV mobility</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Card 1 */}
          <div className="p-8 rounded-3xl bg-slate-900/50 backdrop-blur-xl border border-slate-800/80 hover:border-emerald-500/40 hover:bg-slate-900/70 transition-all group shadow-xl">
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
              <MapPin className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">PostGIS Spatial Discovery</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Find compatible CCS2, Type-2, and CHAdeMO chargers within your exact GPS radius sorted nearest-first using
              curved-Earth spherical calculations.
            </p>
          </div>

          {/* Card 2 */}
          <div className="p-8 rounded-3xl bg-slate-900/50 backdrop-blur-xl border border-slate-800/80 hover:border-teal-500/40 hover:bg-slate-900/70 transition-all group shadow-xl">
            <div className="w-12 h-12 rounded-2xl bg-teal-500/10 text-teal-400 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">Two-Layer Slot Locks</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Eliminate double-booking race conditions. Redis distributed locks hold your 30-minute slot during checkout,
              backed by transactional database overlap checks.
            </p>
          </div>

          {/* Card 3 */}
          <div className="p-8 rounded-3xl bg-slate-900/50 backdrop-blur-xl border border-slate-800/80 hover:border-cyan-500/40 hover:bg-slate-900/70 transition-all group shadow-xl">
            <div className="w-12 h-12 rounded-2xl bg-cyan-500/10 text-cyan-400 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
              <BatteryCharging className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">Live Charging Telemetry</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Scan the charger QR code to start flow. Watch live kilowatt power, delivered kWh, and your live rupee cost
              accumulate on a digital dashboard.
            </p>
          </div>

          {/* Card 4 */}
          <div className="p-8 rounded-3xl bg-slate-900/50 backdrop-blur-xl border border-slate-800/80 hover:border-purple-500/40 hover:bg-slate-900/70 transition-all group shadow-xl">
            <div className="w-12 h-12 rounded-2xl bg-purple-500/10 text-purple-400 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
              <Wallet className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">Host Payouts & Ledger</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Tamper-proof accounting ledger. Hosts earn 90% net revenue from every unit consumed and can withdraw
              directly to their UPI or bank account anytime.
            </p>
          </div>

          {/* Card 5 */}
          <div className="p-8 rounded-3xl bg-slate-900/50 backdrop-blur-xl border border-slate-800/80 hover:border-amber-500/40 hover:bg-slate-900/70 transition-all group shadow-xl">
            <div className="w-12 h-12 rounded-2xl bg-amber-500/10 text-amber-400 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
              <Star className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">Verified Reviews Only</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Zero fake reviews or spam. Only drivers who plugged in and completed an actual charging session are
              authorized to submit 1–5 star ratings.
            </p>
          </div>

          {/* Card 6 */}
          <div className="p-8 rounded-3xl bg-slate-900/50 backdrop-blur-xl border border-slate-800/80 hover:border-emerald-500/40 hover:bg-slate-900/70 transition-all group shadow-xl">
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
              <Sparkles className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">Executive Admin Oversight</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Complete CEO dashboard for platform health: inspect charger hardware photos, approve or reject stations, and
              monitor real-time platform revenue.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
};
