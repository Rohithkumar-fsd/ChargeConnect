import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { Zap, LogOut, Compass, PlusCircle, Wallet, BatteryCharging, Shield, ArrowRight, Car } from 'lucide-react';

export const Navbar = () => {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isActive = (path) => location.pathname === path;

  return (
    <nav className="sticky top-0 z-50 bg-[#061224]/85 backdrop-blur-2xl border-b border-white/[0.08] transition-all">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-18 items-center">
          {/* Logo & Brand */}
          <Link to="/" className="flex items-center gap-3 group">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-[#635bff] via-[#7928ca] to-[#00d4ff] flex items-center justify-center shadow-lg shadow-[#635bff]/30 group-hover:scale-105 group-hover:shadow-[#635bff]/50 transition-all">
              <Zap className="w-5 h-5 text-white fill-current" />
            </div>
            <div className="flex flex-col">
              <span className="font-extrabold text-xl tracking-tight text-white group-hover:text-cyan-300 transition-colors">
                ChargeConnect
              </span>
              <span className="text-[10px] uppercase tracking-widest text-[#00d4ff] font-bold -mt-1">
                P2P EV Network
              </span>
            </div>
          </Link>

          {/* Navigation Links based on role */}
          <div className="hidden md:flex items-center gap-1.5">
            <Link
              to="/"
              className={`px-4 py-2 rounded-full text-sm font-semibold transition-all ${
                isActive('/')
                  ? 'text-white bg-white/10 shadow-inner'
                  : 'text-slate-300 hover:text-white hover:bg-white/[0.06]'
              }`}
            >
              Home
            </Link>

            {/* DRIVER Links */}
            {hasRole('DRIVER') && (
              <>
                <Link
                  to="/driver/vehicles"
                  className={`px-3.5 py-2 rounded-full text-sm font-semibold flex items-center gap-1.5 transition-all ${
                    isActive('/driver/vehicles')
                      ? 'text-cyan-300 bg-white/10'
                      : 'text-slate-300 hover:text-white hover:bg-white/[0.06]'
                  }`}
                >
                  <Car className="w-4 h-4 text-cyan-400" />
                  My EVs
                </Link>
                <Link
                  to="/driver/find"
                  className={`px-3.5 py-2 rounded-full text-sm font-semibold flex items-center gap-1.5 transition-all ${
                    isActive('/driver/find')
                      ? 'text-cyan-300 bg-white/10'
                      : 'text-slate-300 hover:text-white hover:bg-white/[0.06]'
                  }`}
                >
                  <Compass className="w-4 h-4 text-[#00d4ff]" />
                  Find Chargers
                </Link>
                <Link
                  to="/driver/bookings"
                  className={`px-3.5 py-2 rounded-full text-sm font-semibold transition-all ${
                    isActive('/driver/bookings')
                      ? 'text-cyan-300 bg-white/10'
                      : 'text-slate-300 hover:text-white hover:bg-white/[0.06]'
                  }`}
                >
                  My Bookings
                </Link>
                <Link
                  to="/driver/session"
                  className={`px-3.5 py-2 rounded-full text-sm font-semibold flex items-center gap-1.5 transition-all ${
                    isActive('/driver/session')
                      ? 'text-emerald-300 bg-white/10'
                      : 'text-slate-300 hover:text-white hover:bg-white/[0.06]'
                  }`}
                >
                  <BatteryCharging className="w-4 h-4 text-emerald-400 animate-pulse" />
                  Live Charging
                </Link>
              </>
            )}

            {/* HOST Links */}
            {hasRole('HOST') && (
              <>
                <Link
                  to="/host/dashboard"
                  className={`px-4 py-2 rounded-full text-sm font-semibold flex items-center gap-1.5 transition-all ${
                    isActive('/host/dashboard')
                      ? 'text-[#a855f7] bg-white/10'
                      : 'text-slate-300 hover:text-white hover:bg-white/[0.06]'
                  }`}
                >
                  <PlusCircle className="w-4 h-4 text-[#a855f7]" />
                  Host Hub
                </Link>
                <Link
                  to="/host/earnings"
                  className={`px-4 py-2 rounded-full text-sm font-semibold flex items-center gap-1.5 transition-all ${
                    isActive('/host/earnings')
                      ? 'text-[#a855f7] bg-white/10'
                      : 'text-slate-300 hover:text-white hover:bg-white/[0.06]'
                  }`}
                >
                  <Wallet className="w-4 h-4 text-[#a855f7]" />
                  Earnings & Payouts
                </Link>
              </>
            )}

            {/* ADMIN Links */}
            {hasRole('ADMIN') && (
              <Link
                to="/admin"
                className={`px-4 py-2 rounded-full text-sm font-semibold flex items-center gap-1.5 transition-all ${
                  isActive('/admin')
                    ? 'text-amber-300 bg-white/10'
                    : 'text-slate-300 hover:text-white hover:bg-white/[0.06]'
                }`}
              >
                <Shield className="w-4 h-4 text-amber-400" />
                Admin CEO
              </Link>
            )}
          </div>

          {/* User Auth Section */}
          <div className="flex items-center gap-3">
            {user ? (
              <div className="flex items-center gap-3">
                <div className="hidden sm:flex flex-col text-right">
                  <span className="text-sm font-bold text-white">{user.fullName}</span>
                  <div className="flex items-center gap-1 justify-end mt-0.5">
                    {user.roles.map((r) => {
                      const roleName = r.replace('ROLE_', '');
                      const badgeStyle =
                        roleName === 'ADMIN'
                          ? 'bg-amber-500/20 text-amber-300 border-amber-500/40'
                          : roleName === 'HOST'
                          ? 'bg-purple-500/20 text-purple-300 border-purple-500/40'
                          : 'bg-[#00d4ff]/20 text-cyan-300 border-[#00d4ff]/40';
                      return (
                        <span
                          key={r}
                          className={`text-[10px] font-extrabold tracking-wider uppercase px-2.5 py-0.5 rounded-full border ${badgeStyle}`}
                        >
                          {roleName}
                        </span>
                      );
                    })}
                  </div>
                </div>

                <button
                  onClick={handleLogout}
                  className="p-2.5 text-slate-400 hover:text-rose-400 hover:bg-white/[0.08] rounded-full transition-all"
                  title="Logout"
                >
                  <LogOut className="w-5 h-5" />
                </button>
              </div>
            ) : (
              <div className="flex items-center gap-3">
                <Link
                  to="/login"
                  className="px-4 py-2 text-sm font-semibold text-slate-200 hover:text-white transition-colors"
                >
                  Sign in
                </Link>
                <Link
                  to="/register"
                  className="px-5 py-2.5 text-sm font-bold text-white bg-[#635bff] hover:bg-[#5340ff] rounded-full shadow-lg shadow-[#635bff]/40 hover:shadow-[#635bff]/60 hover:scale-[1.03] active:scale-[0.98] transition-all flex items-center gap-1.5"
                >
                  <span>Get Started</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};
