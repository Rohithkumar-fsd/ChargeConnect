import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { Zap, Mail, Lock, User, Phone, AlertCircle, ArrowRight } from 'lucide-react';

export const Register = () => {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const initialRole = searchParams.get('role') === 'HOST' ? 'HOST' : 'DRIVER';

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState(initialRole);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await register(fullName, email, phone, password, role);
      if (role === 'HOST') {
        navigate('/host/dashboard');
      } else {
        navigate('/driver/find');
      }
    } catch (err) {
      setError(err.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
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
          <h1 className="text-3xl font-black text-white tracking-tight">Create your account</h1>
          <p className="text-sm text-slate-400 mt-2">Join the decentralized EV charging marketplace</p>
        </div>

        {/* Register Card (Stripe Glassmorphism) */}
        <div className="p-8 rounded-3xl bg-[#091a33]/85 backdrop-blur-2xl border border-white/[0.12] shadow-2xl shadow-black/60">
          {error && (
            <div className="mb-6 p-4 rounded-2xl bg-rose-500/15 border border-rose-500/30 flex items-start gap-3 text-rose-300 text-sm">
              <AlertCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {/* Role Selector Pill */}
          <div className="mb-6 p-1 rounded-2xl bg-[#061224] border border-white/10 grid grid-cols-2 gap-1">
            <button
              type="button"
              onClick={() => setRole('DRIVER')}
              className={`py-2.5 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-2 ${
                role === 'DRIVER'
                  ? 'bg-[#635bff] text-white shadow-lg shadow-[#635bff]/40'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              🚗 EV Driver
            </button>
            <button
              type="button"
              onClick={() => setRole('HOST')}
              className={`py-2.5 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-2 ${
                role === 'HOST'
                  ? 'bg-purple-600 text-white shadow-lg shadow-purple-600/40'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              ⚡ Charger Host
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-300 mb-1.5">
                Full Name
              </label>
              <div className="relative">
                <User className="w-5 h-5 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  required
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="Rahul Sharma"
                  className="w-full pl-11 pr-4 py-3 bg-[#061224]/80 border border-white/10 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:border-[#635bff] focus:ring-2 focus:ring-[#635bff]/30 transition-all text-sm"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-300 mb-1.5">
                Email Address
              </label>
              <div className="relative">
                <Mail className="w-5 h-5 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="rahul@example.com"
                  className="w-full pl-11 pr-4 py-3 bg-[#061224]/80 border border-white/10 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:border-[#635bff] focus:ring-2 focus:ring-[#635bff]/30 transition-all text-sm"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-300 mb-1.5">
                Phone Number (Optional)
              </label>
              <div className="relative">
                <Phone className="w-5 h-5 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="+91 98765 43210"
                  className="w-full pl-11 pr-4 py-3 bg-[#061224]/80 border border-white/10 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:border-[#635bff] focus:ring-2 focus:ring-[#635bff]/30 transition-all text-sm"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-300 mb-1.5">
                Password
              </label>
              <div className="relative">
                <Lock className="w-5 h-5 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="password"
                  required
                  minLength={8}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="At least 8 characters"
                  className="w-full pl-11 pr-4 py-3 bg-[#061224]/80 border border-white/10 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:border-[#635bff] focus:ring-2 focus:ring-[#635bff]/30 transition-all text-sm"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 bg-[#635bff] hover:bg-[#5340ff] text-white font-bold text-sm rounded-xl shadow-lg shadow-[#635bff]/30 hover:shadow-[#635bff]/50 hover:scale-[1.01] active:scale-[0.99] transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed mt-4"
            >
              {loading ? (
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <>
                  <span>Create Account</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>
        </div>

        {/* Footer Link */}
        <p className="text-center text-sm text-slate-400 mt-6">
          Already have an account?{' '}
          <Link to="/login" className="font-bold text-[#00d4ff] hover:text-cyan-300 transition-colors">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
};
