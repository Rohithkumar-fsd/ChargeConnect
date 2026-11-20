import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { Zap, Mail, Lock, AlertCircle, ArrowRight, Sparkles } from 'lucide-react';

export const Login = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const from = location.state?.from?.pathname || '/';

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const user = await login(email, password);
      // Smart redirect based on primary role if coming from root or login
      if (from === '/' || from === '/login') {
        if (user.roles.includes('ROLE_ADMIN')) {
          navigate('/admin');
        } else if (user.roles.includes('ROLE_HOST')) {
          navigate('/host/dashboard');
        } else {
          navigate('/driver/find');
        }
      } else {
        navigate(from, { replace: true });
      }
    } catch (err) {
      setError(err.message || 'Invalid email or password');
    } finally {
      setLoading(false);
    }
  };

  // Helper function to quickly fill test credentials for learning
  const fillCredentials = (demoEmail, demoPassword) => {
    setEmail(demoEmail);
    setPassword(demoPassword);
    setError('');
  };

  return (
    <div className="min-h-[85vh] flex items-center justify-center px-4 py-12 relative">
      {/* Ambient background glow */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[550px] h-[550px] bg-emerald-500/10 rounded-full blur-3xl pointer-events-none -z-10" />

      <div className="w-full max-w-md">
        {/* Card Header */}
        <div className="text-center mb-8">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-emerald-400 to-teal-400 flex items-center justify-center mx-auto mb-4 shadow-xl shadow-emerald-500/25">
            <Zap className="w-6 h-6 text-slate-950 fill-current" />
          </div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight">Welcome back</h1>
          <p className="text-sm text-slate-400 mt-1">Sign in to manage your charging or host dashboard</p>
        </div>

        {/* Login Card (Glassmorphism) */}
        <div className="p-8 rounded-3xl bg-slate-900/70 backdrop-blur-2xl border border-slate-800/80 shadow-2xl shadow-black/50">
          {error && (
            <div className="mb-6 p-4 rounded-2xl bg-rose-500/10 border border-rose-500/20 flex items-start gap-3 text-rose-300 text-sm">
              <AlertCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-2">
                Email Address
              </label>
              <div className="relative">
                <Mail className="w-5 h-5 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  className="w-full pl-11 pr-4 py-3 bg-slate-950/70 border border-slate-800 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-all text-sm"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-2">
                Password
              </label>
              <div className="relative">
                <Lock className="w-5 h-5 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  className="w-full pl-11 pr-4 py-3 bg-slate-950/70 border border-slate-800 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-all text-sm"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 bg-gradient-to-r from-emerald-400 to-teal-400 hover:from-emerald-300 hover:to-teal-300 text-slate-950 font-bold text-sm rounded-xl shadow-lg shadow-emerald-500/20 hover:shadow-emerald-500/30 transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed mt-2"
            >
              {loading ? (
                <div className="w-5 h-5 border-2 border-slate-950/30 border-t-slate-950 rounded-full animate-spin" />
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Quick Demo Fill Buttons (Super helpful for student testing) */}
          <div className="mt-8 pt-6 border-t border-slate-800/60">
            <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-400 mb-3">
              <Sparkles className="w-3.5 h-3.5 text-emerald-400" />
              <span>Quick-Fill Demo Test Accounts:</span>
            </div>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => fillCredentials('driver.e2e@test.com', 'Password123!')}
                className="px-2.5 py-1.5 rounded-lg bg-slate-800/60 hover:bg-slate-800 text-[11px] font-medium text-emerald-300 border border-emerald-500/20 transition-all text-center"
              >
                🚗 Driver
              </button>
              <button
                type="button"
                onClick={() => fillCredentials('host.e2e@test.com', 'Password123!')}
                className="px-2.5 py-1.5 rounded-lg bg-slate-800/60 hover:bg-slate-800 text-[11px] font-medium text-purple-300 border border-purple-500/20 transition-all text-center"
              >
                ⚡ Host
              </button>
              <button
                type="button"
                onClick={() => fillCredentials('admin@chargeconnect.com', 'AdminPass123!')}
                className="px-2.5 py-1.5 rounded-lg bg-slate-800/60 hover:bg-slate-800 text-[11px] font-medium text-amber-300 border border-amber-500/20 transition-all text-center"
              >
                👑 Admin
              </button>
            </div>
          </div>
        </div>

        {/* Footer Link */}
        <p className="text-center text-sm text-slate-400 mt-6">
          Don't have an account yet?{' '}
          <Link to="/register" className="font-semibold text-emerald-400 hover:text-emerald-300 transition-colors">
            Create an account
          </Link>
        </p>
      </div>
    </div>
  );
};
