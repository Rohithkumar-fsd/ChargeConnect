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

  const fillCredentials = (demoEmail, demoPassword) => {
    setEmail(demoEmail);
    setPassword(demoPassword);
    setError('');
  };

  return (
    <div className="min-h-[85vh] flex items-center justify-center px-4 py-16 relative bg-[#061224]">
      {/* Stripe Ambient Background Mesh Glow */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-[700px] bg-gradient-to-tr from-[#635bff]/20 via-[#a855f7]/15 to-[#00d4ff]/10 rounded-full blur-[140px] pointer-events-none -z-10" />

      <div className="w-full max-w-md">
        {/* Card Header */}
        <div className="text-center mb-8">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-[#635bff] to-[#00d4ff] flex items-center justify-center mx-auto mb-4 shadow-xl shadow-[#635bff]/30">
            <Zap className="w-6 h-6 text-white fill-current" />
          </div>
          <h1 className="text-3xl font-black text-white tracking-tight">Sign in to ChargeConnect</h1>
          <p className="text-sm text-slate-400 mt-2">Manage your EV charging sessions or host payouts</p>
        </div>

        {/* Login Card (Stripe Glassmorphism) */}
        <div className="p-8 rounded-3xl bg-[#091a33]/85 backdrop-blur-2xl border border-white/[0.12] shadow-2xl shadow-black/60">
          {error && (
            <div className="mb-6 p-4 rounded-2xl bg-rose-500/15 border border-rose-500/30 flex items-start gap-3 text-rose-300 text-sm">
              <AlertCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-300 mb-2">
                Email Address
              </label>
              <div className="relative">
                <Mail className="w-5 h-5 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  className="w-full pl-11 pr-4 py-3 bg-[#061224]/80 border border-white/10 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:border-[#635bff] focus:ring-2 focus:ring-[#635bff]/30 transition-all text-sm"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-300 mb-2">
                Password
              </label>
              <div className="relative">
                <Lock className="w-5 h-5 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  className="w-full pl-11 pr-4 py-3 bg-[#061224]/80 border border-white/10 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:border-[#635bff] focus:ring-2 focus:ring-[#635bff]/30 transition-all text-sm"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 bg-[#635bff] hover:bg-[#5340ff] text-white font-bold text-sm rounded-xl shadow-lg shadow-[#635bff]/30 hover:shadow-[#635bff]/50 hover:scale-[1.01] active:scale-[0.99] transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed mt-3"
            >
              {loading ? (
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Quick Demo Fill Buttons */}
          <div className="mt-8 pt-6 border-t border-white/[0.08]">
            <div className="flex items-center gap-1.5 text-xs font-bold text-slate-400 mb-3">
              <Sparkles className="w-3.5 h-3.5 text-cyan-400" />
              <span>1-Click Test Accounts:</span>
            </div>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => fillCredentials('driver.e2e@test.com', 'Password123!')}
                className="px-2.5 py-2 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-[11px] font-bold text-cyan-300 border border-cyan-500/30 transition-all text-center"
              >
                🚗 Driver
              </button>
              <button
                type="button"
                onClick={() => fillCredentials('host.e2e@test.com', 'Password123!')}
                className="px-2.5 py-2 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-[11px] font-bold text-purple-300 border border-purple-500/30 transition-all text-center"
              >
                ⚡ Host
              </button>
              <button
                type="button"
                onClick={() => fillCredentials('admin@chargeconnect.com', 'Password123!')}
                className="px-2.5 py-2 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-[11px] font-bold text-amber-300 border border-amber-500/30 transition-all text-center"
              >
                👑 Admin
              </button>
            </div>
          </div>
        </div>

        {/* Footer Link */}
        <p className="text-center text-sm text-slate-400 mt-6">
          Don't have an account yet?{' '}
          <Link to="/register" className="font-bold text-[#00d4ff] hover:text-cyan-300 transition-colors">
            Sign up now
          </Link>
        </p>
      </div>
    </div>
  );
};
