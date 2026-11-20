import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { Zap, LogOut, Compass, PlusCircle, Wallet, BatteryCharging, Shield } from 'lucide-react';

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
    <nav className="sticky top-0 z-50 bg-slate-950/80 backdrop-blur-xl border-b border-slate-800/80">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16 items-center">
          {/* Logo & Brand */}
          <Link to="/" className="flex items-center gap-2.5 group">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-emerald-400 via-teal-500 to-cyan-400 flex items-center justify-center shadow-lg shadow-emerald-500/20 group-hover:scale-105 group-hover:shadow-emerald-500/30 transition-all">
              <Zap className="w-5 h-5 text-slate-950 fill-current" />
            </div>
            <div className="flex flex-col">
              <span className="font-extrabold text-lg tracking-tight bg-gradient-to-r from-white via-slate-100 to-slate-400 bg-clip-text text-transparent">
                ChargeConnect
              </span>
              <span className="text-[10px] uppercase tracking-widest text-emerald-400 font-semibold -mt-1">
                P2P EV Marketplace
              </span>
            </div>
          </Link>

          {/* Navigation Links based on role */}
          <div className="hidden md:flex items-center gap-1.5">
            <Link
              to="/"
              className={`px-3.5 py-2 rounded-xl text-sm font-medium transition-all ${
                isActive('/')
                  ? 'text-emerald-400 bg-slate-800/80 shadow-inner'
                  : 'text-slate-300 hover:text-white hover:bg-slate-800/40'
              }`}
            >
              Home
            </Link>

            {/* DRIVER Links */}
            {hasRole('DRIVER') && (
              <>
                <Link
                  to="/driver/find"
                  className={`px-3.5 py-2 rounded-xl text-sm font-medium flex items-center gap-1.5 transition-all ${
                    isActive('/driver/find')
                      ? 'text-emerald-400 bg-slate-800/80'
                      : 'text-slate-300 hover:text-white hover:bg-slate-800/40'
                  }`}
                >
                  <Compass className="w-4 h-4 text-emerald-400" />
                  Find Chargers
                </Link>
                <Link
                  to="/driver/bookings"
                  className={`px-3.5 py-2 rounded-xl text-sm font-medium transition-all ${
                    isActive('/driver/bookings')
                      ? 'text-emerald-400 bg-slate-800/80'
                      : 'text-slate-300 hover:text-white hover:bg-slate-800/40'
                  }`}
                >
                  My Bookings
                </Link>
                <Link
                  to="/driver/session"
                  className={`px-3.5 py-2 rounded-xl text-sm font-medium flex items-center gap-1.5 transition-all ${
                    isActive('/driver/session')
                      ? 'text-emerald-400 bg-slate-800/80'
                      : 'text-slate-300 hover:text-white hover:bg-slate-800/40'
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
                  className={`px-3.5 py-2 rounded-xl text-sm font-medium flex items-center gap-1.5 transition-all ${
                    isActive('/host/dashboard')
                      ? 'text-purple-400 bg-slate-800/80'
                      : 'text-slate-300 hover:text-white hover:bg-slate-800/40'
                  }`}
                >
                  <PlusCircle className="w-4 h-4 text-purple-400" />
                  Host Hub
                </Link>
                <Link
                  to="/host/earnings"
                  className={`px-3.5 py-2 rounded-xl text-sm font-medium flex items-center gap-1.5 transition-all ${
                    isActive('/host/earnings')
                      ? 'text-purple-400 bg-slate-800/80'
                      : 'text-slate-300 hover:text-white hover:bg-slate-800/40'
                  }`}
                >
                  <Wallet className="w-4 h-4 text-purple-400" />
                  Earnings & Payouts
                </Link>
              </>
            )}

            {/* ADMIN Links */}
            {hasRole('ADMIN') && (
              <Link
                to="/admin"
                className={`px-3.5 py-2 rounded-xl text-sm font-medium flex items-center gap-1.5 transition-all ${
                  isActive('/admin')
                    ? 'text-amber-400 bg-slate-800/80'
                    : 'text-slate-300 hover:text-white hover:bg-slate-800/40'
                }`}
              >
                <Shield className="w-4 h-4 text-amber-400" />
                Admin Dashboard
              </Link>
            )}
          </div>

          {/* User Auth Section */}
          <div className="flex items-center gap-3">
            {user ? (
              <div className="flex items-center gap-3">
                <div className="hidden sm:flex flex-col text-right">
                  <span className="text-sm font-semibold text-white">{user.fullName}</span>
                  <div className="flex items-center gap-1 justify-end mt-0.5">
                    {user.roles.map((r) => {
                      const roleName = r.replace('ROLE_', '');
                      const badgeStyle =
                        roleName === 'ADMIN'
                          ? 'bg-amber-500/10 text-amber-300 border-amber-500/30'
                          : roleName === 'HOST'
                          ? 'bg-purple-500/10 text-purple-300 border-purple-500/30'
                          : 'bg-emerald-500/10 text-emerald-300 border-emerald-500/30';
                      return (
                        <span
                          key={r}
                          className={`text-[10px] font-bold tracking-wide uppercase px-2 py-0.5 rounded-full border ${badgeStyle}`}
                        >
                          {roleName}
                        </span>
                      );
                    })}
                  </div>
                </div>

                <button
                  onClick={handleLogout}
                  className="p-2 text-slate-400 hover:text-rose-400 hover:bg-slate-800/60 rounded-xl transition-all"
                  title="Logout"
                >
                  <LogOut className="w-5 h-5" />
                </button>
              </div>
            ) : (
              <div className="flex items-center gap-2.5">
                <Link
                  to="/login"
                  className="px-4 py-2 text-sm font-medium text-slate-300 hover:text-white hover:bg-slate-800/60 rounded-xl transition-all"
                >
                  Sign in
                </Link>
                <Link
                  to="/register"
                  className="px-4 py-2 text-sm font-semibold text-slate-950 bg-gradient-to-r from-emerald-400 to-teal-400 hover:from-emerald-300 hover:to-teal-300 rounded-xl shadow-lg shadow-emerald-500/20 hover:shadow-emerald-500/30 hover:scale-[1.02] active:scale-[0.98] transition-all"
                >
                  Get Started
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};
