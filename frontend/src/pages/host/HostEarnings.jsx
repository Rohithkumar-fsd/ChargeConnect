import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client.js';
import {
  Wallet,
  ArrowUpRight,
  Clock,
  CheckCircle2,
  AlertCircle,
  X,
  CreditCard,
  Building,
  Smartphone,
  Receipt,
  Sparkles,
  ShieldCheck,
  TrendingUp,
} from 'lucide-react';

export const HostEarnings = () => {
  const [summary, setSummary] = useState(null);
  const [payouts, setPayouts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Request Payout Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [submittingPayout, setSubmittingPayout] = useState(false);
  const [payoutAmount, setPayoutAmount] = useState('');
  const [payoutMethod, setPayoutMethod] = useState('UPI');
  const [accountDetails, setAccountDetails] = useState('host@okhdfcbank');

  const fetchData = async () => {
    try {
      setLoading(true);
      setError('');
      const [earningsRes, payoutsRes] = await Promise.all([
        api.get('/api/host/payouts/earnings'),
        api.get('/api/host/payouts'),
      ]);
      setSummary(earningsRes);
      setPayouts(Array.isArray(payoutsRes) ? payoutsRes : []);
    } catch (err) {
      setError(err.message || 'Failed to load earnings & payout ledger.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleRequestPayout = async (e) => {
    e.preventDefault();
    const amountVal = parseFloat(payoutAmount);
    if (isNaN(amountVal) || amountVal < 50.0) {
      setError('Minimum withdrawal amount is ₹50.00.');
      return;
    }
    if (summary?.availableBalance && amountVal > parseFloat(summary.availableBalance)) {
      setError(`Requested amount exceeds available balance (₹${summary.availableBalance}).`);
      return;
    }

    try {
      setSubmittingPayout(true);
      setError('');
      await api.post('/api/host/payouts/request', {
        amount: amountVal,
        payoutMethod: payoutMethod,
        accountDetails: accountDetails.trim(),
      });
      setSuccess(`Payout request for ₹${amountVal.toFixed(2)} submitted to Admin!`);
      setIsModalOpen(false);
      setPayoutAmount('');
      await fetchData();
    } catch (err) {
      setError(err.message || 'Failed to request payout.');
    } finally {
      setSubmittingPayout(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'PROCESSED':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-500/10 text-emerald-300 border border-emerald-500/30">
            Processed
          </span>
        );
      case 'PENDING':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-amber-500/10 text-amber-300 border border-amber-500/30">
            Pending Approval
          </span>
        );
      case 'REJECTED':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-rose-500/10 text-rose-300 border border-rose-500/30">
            Rejected
          </span>
        );
      default:
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-slate-500/10 text-slate-400">
            {status}
          </span>
        );
    }
  };

  return (
    <div className="relative min-h-[calc(100vh-4.5rem)] bg-[#061224] py-10 px-4 sm:px-6 lg:px-8">
      {/* Background glow meshes */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-[#7928ca]/15 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-[#00d4ff]/10 rounded-full blur-3xl pointer-events-none" />

      <div className="relative max-w-6xl mx-auto space-y-8">
        {/* Header Bar */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-white/[0.08] pb-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-purple-500/10 border border-purple-500/20 text-purple-300 text-xs font-semibold mb-2">
              <TrendingUp className="w-3.5 h-3.5" />
              Dynamic Accounting Ledger
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
              Host Earnings & Payouts
            </h1>
            <p className="text-slate-400 text-sm mt-1">
              Transparent real-time balance calculations, platform commissions, and instant UPI withdrawals.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <Link
              to="/host/dashboard"
              className="px-4 py-2.5 rounded-full bg-white/[0.06] hover:bg-white/[0.1] text-white font-semibold text-xs border border-white/10 transition-colors"
            >
              ← Station Fleet Hub
            </Link>
            <button
              onClick={() => setIsModalOpen(true)}
              disabled={!summary || parseFloat(summary.availableBalance || 0) < 50}
              className="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-full bg-gradient-to-r from-emerald-500 to-cyan-500 text-white font-semibold text-xs shadow-lg shadow-emerald-500/25 hover:scale-[1.02] active:scale-[0.98] disabled:opacity-50 transition-all"
            >
              <ArrowUpRight className="w-4 h-4" />
              Withdraw Payout
            </button>
          </div>
        </div>

        {/* Notifications */}
        {error && (
          <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm flex items-center justify-between">
            <div className="flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
            <button onClick={() => setError('')} className="text-rose-400 hover:text-white">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {success && (
          <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-sm flex items-center justify-between">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>{success}</span>
            </div>
            <button onClick={() => setSuccess('')} className="text-emerald-400 hover:text-white">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Financial KPI Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Main Wallet Card */}
          <div className="sm:col-span-2 rounded-3xl border border-emerald-500/30 bg-gradient-to-br from-emerald-950/20 via-[#071328] to-[#071328] backdrop-blur-xl p-6 shadow-xl flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs uppercase tracking-wider text-emerald-300 font-semibold flex items-center gap-1.5">
                  <Wallet className="w-4 h-4 text-emerald-400" />
                  Available Wallet Balance
                </span>
                <span className="px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 text-[10px] font-bold">
                  Ready to Withdraw
                </span>
              </div>
              <div className="text-4xl font-extrabold text-white font-mono mt-1">
                ₹{summary ? summary.availableBalance : '0.00'}
              </div>
              <p className="text-xs text-slate-400 mt-2">
                Net earnings minus previously settled and pending payouts.
              </p>
            </div>

            <div className="pt-4 mt-4 border-t border-white/[0.08] flex items-center justify-between text-xs text-slate-300">
              <span>Pending in Review:</span>
              <span className="font-mono text-amber-400 font-semibold">
                ₹{summary ? summary.pendingPayouts : '0.00'}
              </span>
            </div>
          </div>

          {/* Gross Revenue */}
          <div className="rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl p-6 flex flex-col justify-between">
            <div>
              <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold">
                Gross Inflow
              </span>
              <div className="text-2xl font-extrabold text-white font-mono mt-2">
                ₹{summary ? summary.grossEarnings : '0.00'}
              </div>
            </div>
            <div className="text-[11px] text-slate-400 pt-3 border-t border-white/[0.06]">
              Total charged from drivers
            </div>
          </div>

          {/* Net Host Earnings */}
          <div className="rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl p-6 flex flex-col justify-between">
            <div>
              <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold">
                Net Host Earnings
              </span>
              <div className="text-2xl font-extrabold text-cyan-300 font-mono mt-2">
                ₹{summary ? summary.netEarnings : '0.00'}
              </div>
            </div>
            <div className="text-[11px] text-slate-400 pt-3 border-t border-white/[0.06]">
              After platform commission (₹{summary ? summary.platformCommission : '0.00'})
            </div>
          </div>
        </div>

        {/* Payout History Ledger */}
        <div className="rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl p-6 sm:p-8 shadow-xl">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="text-lg font-bold text-white">Withdrawal History</h3>
              <p className="text-xs text-slate-400 mt-0.5">
                Audit trail of bank and UPI payouts requested from your account.
              </p>
            </div>
            <button
              onClick={fetchData}
              className="text-xs text-cyan-400 hover:text-white transition-colors"
            >
              Refresh Ledger
            </button>
          </div>

          {loading ? (
            <div className="py-12 text-center text-slate-400 text-xs">
              <div className="inline-block w-6 h-6 border-2 border-purple-400 border-t-transparent rounded-full animate-spin mb-2" />
              <p>Loading ledger entries...</p>
            </div>
          ) : payouts.length === 0 ? (
            <div className="p-8 rounded-2xl bg-white/[0.02] border border-white/10 text-center text-slate-400 text-xs">
              <Receipt className="w-8 h-8 text-slate-600 mx-auto mb-2" />
              No withdrawal requests placed yet.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-white/[0.08] text-slate-400 uppercase tracking-wider text-[11px]">
                    <th className="pb-3 font-semibold">Reference</th>
                    <th className="pb-3 font-semibold">Date</th>
                    <th className="pb-3 font-semibold">Amount</th>
                    <th className="pb-3 font-semibold">Method</th>
                    <th className="pb-3 font-semibold">Account / UPI</th>
                    <th className="pb-3 font-semibold">Status</th>
                    <th className="pb-3 font-semibold">UTR / Remarks</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/[0.04]">
                  {payouts.map((p) => (
                    <tr key={p.id} className="hover:bg-white/[0.02] transition-colors">
                      <td className="py-3.5 font-mono text-cyan-300 font-semibold">
                        {p.payoutReference}
                      </td>
                      <td className="py-3.5 text-slate-400">
                        {new Date(p.createdAt).toLocaleDateString([], {
                          month: 'short',
                          day: 'numeric',
                          year: 'numeric',
                        })}
                      </td>
                      <td className="py-3.5 font-bold text-white font-mono">
                        ₹{p.amount}
                      </td>
                      <td className="py-3.5 text-slate-300 font-medium">
                        {p.payoutMethod}
                      </td>
                      <td className="py-3.5 font-mono text-slate-400">
                        {p.accountDetails}
                      </td>
                      <td className="py-3.5">
                        {getStatusBadge(p.status)}
                      </td>
                      <td className="py-3.5 font-mono text-slate-300">
                        {p.transactionReference || p.failureReason || '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Request Payout Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md">
          <div className="relative w-full max-w-md rounded-3xl border border-white/[0.12] bg-[#071328] p-6 sm:p-8 shadow-2xl text-white">
            <button
              onClick={() => setIsModalOpen(false)}
              className="absolute top-6 right-6 p-2 rounded-xl text-slate-400 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="mb-6">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/20 border border-emerald-500/40 text-emerald-300 text-xs font-semibold mb-2">
                <ArrowUpRight className="w-3.5 h-3.5" />
                Transfer Funds
              </div>
              <h2 className="text-2xl font-bold">Request Host Payout</h2>
              <p className="text-slate-400 text-xs mt-1">
                Available to withdraw:{' '}
                <strong className="text-white font-mono">₹{summary?.availableBalance}</strong>
              </p>
            </div>

            <form onSubmit={handleRequestPayout} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">
                  Withdrawal Amount (₹) *
                </label>
                <input
                  type="number"
                  step="1"
                  min="50"
                  max={summary?.availableBalance || 100000}
                  required
                  placeholder="e.g. 500"
                  value={payoutAmount}
                  onChange={(e) => setPayoutAmount(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-white font-mono text-sm focus:outline-none focus:border-emerald-400"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">
                  Transfer Method *
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      setPayoutMethod('UPI');
                      setAccountDetails('host@okhdfcbank');
                    }}
                    className={`p-3 rounded-xl border text-xs font-semibold flex items-center justify-center gap-2 transition-all ${
                      payoutMethod === 'UPI'
                        ? 'bg-emerald-500/20 border-emerald-400 text-emerald-200'
                        : 'bg-white/[0.02] border-white/10 text-slate-400'
                    }`}
                  >
                    <Smartphone className="w-4 h-4" />
                    Instant UPI
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setPayoutMethod('BANK_TRANSFER');
                      setAccountDetails('HDFC0001234 / 5010049281726');
                    }}
                    className={`p-3 rounded-xl border text-xs font-semibold flex items-center justify-center gap-2 transition-all ${
                      payoutMethod === 'BANK_TRANSFER'
                        ? 'bg-emerald-500/20 border-emerald-400 text-emerald-200'
                        : 'bg-white/[0.02] border-white/10 text-slate-400'
                    }`}
                  >
                    <Building className="w-4 h-4" />
                    NEFT / IMPS
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">
                  {payoutMethod === 'UPI' ? 'UPI Virtual Payment Address (VPA) *' : 'Bank IFSC & Account Number *'}
                </label>
                <input
                  type="text"
                  required
                  placeholder={payoutMethod === 'UPI' ? 'e.g. username@okhdfcbank' : 'e.g. HDFC0001234 / 50100...'}
                  value={accountDetails}
                  onChange={(e) => setAccountDetails(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-white font-mono text-xs focus:outline-none focus:border-emerald-400"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-white/[0.08]">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 text-slate-400 hover:text-white text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingPayout}
                  className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-emerald-500 to-cyan-500 text-white font-bold text-xs shadow-lg shadow-emerald-500/25 transition-all flex items-center gap-2"
                >
                  {submittingPayout ? 'Submitting...' : 'Confirm Withdrawal'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
