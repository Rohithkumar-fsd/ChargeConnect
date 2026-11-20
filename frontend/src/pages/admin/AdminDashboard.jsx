import React, { useState, useEffect } from 'react';
import { api } from '../../api/client.js';
import {
  Shield,
  TrendingUp,
  Zap,
  Users,
  CheckCircle2,
  AlertCircle,
  X,
  Clock,
  Sparkles,
  Search,
  Check,
  Ban,
  Building,
  DollarSign,
  Activity,
  Layers,
  ArrowRight,
} from 'lucide-react';

export const AdminDashboard = () => {
  const [activeTab, setActiveTab] = useState('ANALYTICS'); // ANALYTICS | CHARGERS | PAYOUTS | USERS

  // Analytics
  const [analytics, setAnalytics] = useState(null);
  const [loadingAnalytics, setLoadingAnalytics] = useState(true);

  // Chargers
  const [chargers, setChargers] = useState([]);
  const [loadingChargers, setLoadingChargers] = useState(false);
  const [rejectingCharger, setRejectingCharger] = useState(null);
  const [rejectionReason, setRejectionReason] = useState('');

  // Payouts
  const [payouts, setPayouts] = useState([]);
  const [loadingPayouts, setLoadingPayouts] = useState(false);
  const [processingPayout, setProcessingPayout] = useState(null);
  const [transactionRef, setTransactionRef] = useState('');
  const [rejectingPayout, setRejectingPayout] = useState(null);
  const [payoutRejectReason, setPayoutRejectReason] = useState('');

  // Users
  const [users, setUsers] = useState([]);
  const [loadingUsers, setLoadingUsers] = useState(false);

  // Feedback notifications
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // 1. Fetch Analytics Overview
  const fetchAnalytics = async () => {
    try {
      setLoadingAnalytics(true);
      const data = await api.get('/api/admin/analytics');
      setAnalytics(data);
    } catch (err) {
      setError('Failed to fetch platform analytics.');
    } finally {
      setLoadingAnalytics(false);
    }
  };

  // 2. Fetch Chargers
  const fetchChargers = async () => {
    try {
      setLoadingChargers(true);
      const data = await api.get('/api/admin/chargers');
      setChargers(Array.isArray(data) ? data : []);
    } catch (err) {
      setError('Failed to fetch chargers.');
    } finally {
      setLoadingChargers(false);
    }
  };

  // 3. Fetch Payouts
  const fetchPayouts = async () => {
    try {
      setLoadingPayouts(true);
      const data = await api.get('/api/admin/payouts');
      setPayouts(Array.isArray(data) ? data : []);
    } catch (err) {
      setError('Failed to fetch payouts.');
    } finally {
      setLoadingPayouts(false);
    }
  };

  // 4. Fetch Users
  const fetchUsers = async () => {
    try {
      setLoadingUsers(true);
      const data = await api.get('/api/admin/users');
      setUsers(Array.isArray(data) ? data : []);
    } catch (err) {
      setError('Failed to fetch platform users.');
    } finally {
      setLoadingUsers(false);
    }
  };

  useEffect(() => {
    fetchAnalytics();
  }, []);

  // Fetch when tab switches
  useEffect(() => {
    setError('');
    setSuccess('');
    if (activeTab === 'ANALYTICS') fetchAnalytics();
    if (activeTab === 'CHARGERS') fetchChargers();
    if (activeTab === 'PAYOUTS') fetchPayouts();
    if (activeTab === 'USERS') fetchUsers();
  }, [activeTab]);

  // Charger Verification Action
  const handleVerifyCharger = async (chargerId, decision, reason = '') => {
    try {
      setError('');
      await api.patch(`/api/admin/chargers/${chargerId}/verification`, {
        decision: decision,
        rejectionReason: reason ? reason.trim() : undefined,
      });
      setSuccess(`Station ${decision === 'APPROVE' ? 'Approved' : 'Rejected'} successfully!`);
      setRejectingCharger(null);
      setRejectionReason('');
      await fetchChargers();
      fetchAnalytics();
    } catch (err) {
      setError(err.message || 'Verification update failed.');
    }
  };

  // Process Payout Action
  const handleProcessPayout = async (e) => {
    e.preventDefault();
    if (!processingPayout) return;
    try {
      setError('');
      await api.post(`/api/admin/payouts/${processingPayout.id}/process`, {
        transactionReference: transactionRef.trim() || `UTR-${Date.now()}`,
      });
      setSuccess(`Payout #${processingPayout.payoutReference} processed! Funds disbursed.`);
      setProcessingPayout(null);
      setTransactionRef('');
      await fetchPayouts();
      fetchAnalytics();
    } catch (err) {
      setError(err.message || 'Failed to process payout.');
    }
  };

  // Reject Payout Action
  const handleRejectPayout = async (e) => {
    e.preventDefault();
    if (!rejectingPayout) return;
    try {
      setError('');
      await api.post(`/api/admin/payouts/${rejectingPayout.id}/reject`, {
        reason: payoutRejectReason.trim() || 'Admin rejected payout request',
      });
      setSuccess(`Payout #${rejectingPayout.payoutReference} rejected.`);
      setRejectingPayout(null);
      setPayoutRejectReason('');
      await fetchPayouts();
      fetchAnalytics();
    } catch (err) {
      setError(err.message || 'Failed to reject payout.');
    }
  };

  // Toggle User Status
  const handleToggleUserStatus = async (userId, currentStatus) => {
    const nextStatus = currentStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    try {
      setError('');
      await api.patch(`/api/admin/users/${userId}/status`, {
        status: nextStatus,
      });
      setSuccess(`User status changed to ${nextStatus}.`);
      setUsers((prev) =>
        prev.map((u) => (u.id === userId ? { ...u, status: nextStatus } : u))
      );
    } catch (err) {
      setError(err.message || 'Could not update user status.');
    }
  };

  return (
    <div className="relative min-h-[calc(100vh-4.5rem)] bg-[#061224] py-10 px-4 sm:px-6 lg:px-8">
      {/* Background glow meshes */}
      <div className="absolute top-1/4 left-1/3 w-96 h-96 bg-[#ff6b9d]/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 right-1/3 w-96 h-96 bg-[#635bff]/15 rounded-full blur-3xl pointer-events-none" />

      <div className="relative max-w-7xl mx-auto space-y-8">
        {/* Header Bar */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-white/[0.08] pb-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-300 text-xs font-semibold mb-2">
              <Shield className="w-3.5 h-3.5" />
              Chief Executive & Compliance Cockpit
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
              Platform Admin CEO
            </h1>
            <p className="text-slate-400 text-sm mt-1">
              Global marketplace health, station hardware verification, payout disbursements, and user governance.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-emerald-500/10 text-emerald-300 text-xs font-semibold border border-emerald-500/30">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              Platform Operational
            </span>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex gap-2 border-b border-white/[0.08] pb-4 overflow-x-auto">
          {[
            { id: 'ANALYTICS', label: '📊 Executive Analytics' },
            { id: 'CHARGERS', label: '⚡ Charger Audits' },
            { id: 'PAYOUTS', label: '💸 Payout Approvals' },
            { id: 'USERS', label: '👥 User Directory' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all ${
                activeTab === tab.id
                  ? 'bg-gradient-to-r from-[#635bff] to-[#00d4ff] text-white shadow-lg shadow-[#635bff]/25'
                  : 'text-slate-400 hover:text-white hover:bg-white/[0.04]'
              }`}
            >
              {tab.label}
            </button>
          ))}
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

        {/* ----------------- TAB 1: EXECUTIVE ANALYTICS ----------------- */}
        {activeTab === 'ANALYTICS' && (
          <div className="space-y-6">
            {loadingAnalytics ? (
              <div className="py-20 text-center">
                <div className="inline-block w-8 h-8 border-4 border-amber-400 border-t-transparent rounded-full animate-spin mb-4" />
                <p className="text-slate-400 text-sm">Aggregating platform metrics...</p>
              </div>
            ) : analytics ? (
              <>
                {/* Top Financial Stats */}
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                  <div className="p-6 rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl">
                    <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold flex items-center gap-1.5">
                      <DollarSign className="w-3.5 h-3.5 text-emerald-400" />
                      Gross Volume (GMV)
                    </span>
                    <div className="text-3xl font-extrabold text-white font-mono mt-2">
                      ₹{analytics.totalGrossVolume}
                    </div>
                    <div className="text-[11px] text-slate-400 mt-2">
                      Total driver billing throughput
                    </div>
                  </div>

                  <div className="p-6 rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl">
                    <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold flex items-center gap-1.5">
                      <TrendingUp className="w-3.5 h-3.5 text-cyan-400" />
                      Net Platform Commission
                    </span>
                    <div className="text-3xl font-extrabold text-cyan-300 font-mono mt-2">
                      ₹{analytics.platformCommissionEarned}
                    </div>
                    <div className="text-[11px] text-slate-400 mt-2">
                      Platform fee retained
                    </div>
                  </div>

                  <div className="p-6 rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl">
                    <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold flex items-center gap-1.5">
                      <Zap className="w-3.5 h-3.5 text-amber-400" />
                      Energy Delivered
                    </span>
                    <div className="text-3xl font-extrabold text-white font-mono mt-2">
                      {analytics.totalEnergyDeliveredKwh} kWh
                    </div>
                    <div className="text-[11px] text-slate-400 mt-2">
                      Across all verified chargers
                    </div>
                  </div>

                  <div className="p-6 rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl">
                    <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold flex items-center gap-1.5">
                      <Activity className="w-3.5 h-3.5 text-purple-400" />
                      Sessions Handled
                    </span>
                    <div className="text-3xl font-extrabold text-purple-300 font-mono mt-2">
                      {analytics.totalSessions}
                    </div>
                    <div className="text-[11px] text-slate-400 mt-2">
                      {analytics.activeSessions} currently active • {analytics.completedSessions} completed
                    </div>
                  </div>
                </div>

                {/* Second Row: Community & Station Fleets */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {/* Community Breakdown */}
                  <div className="p-6 rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl">
                    <h3 className="text-base font-bold text-white mb-4 flex items-center gap-2">
                      <Users className="w-4 h-4 text-cyan-400" />
                      User Community Distribution
                    </h3>
                    <div className="space-y-3 text-xs">
                      <div className="flex justify-between p-3 rounded-xl bg-white/[0.02]">
                        <span className="text-slate-400">Total Registered Accounts:</span>
                        <span className="font-bold text-white">{analytics.totalUsers}</span>
                      </div>
                      <div className="flex justify-between p-3 rounded-xl bg-white/[0.02]">
                        <span className="text-slate-400">EV Drivers:</span>
                        <span className="font-bold text-cyan-300">{analytics.totalDrivers}</span>
                      </div>
                      <div className="flex justify-between p-3 rounded-xl bg-white/[0.02]">
                        <span className="text-slate-400">Charger Hosts:</span>
                        <span className="font-bold text-purple-300">{analytics.totalHosts}</span>
                      </div>
                    </div>
                  </div>

                  {/* Charger Fleet Verification Posture */}
                  <div className="p-6 rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl">
                    <h3 className="text-base font-bold text-white mb-4 flex items-center gap-2">
                      <Layers className="w-4 h-4 text-emerald-400" />
                      Station Fleet Health
                    </h3>
                    <div className="space-y-3 text-xs">
                      <div className="flex justify-between p-3 rounded-xl bg-white/[0.02]">
                        <span className="text-slate-400">Total Stations Registered:</span>
                        <span className="font-bold text-white">{analytics.totalChargers}</span>
                      </div>
                      <div className="flex justify-between p-3 rounded-xl bg-white/[0.02]">
                        <span className="text-slate-400">Approved & Live on Map:</span>
                        <span className="font-bold text-emerald-400">{analytics.approvedChargers}</span>
                      </div>
                      <div className="flex justify-between p-3 rounded-xl bg-white/[0.02]">
                        <span className="text-slate-400">Pending Safety Audit:</span>
                        <span className="font-bold text-amber-400">{analytics.pendingChargers}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </>
            ) : null}
          </div>
        )}

        {/* ----------------- TAB 2: CHARGER AUDITS ----------------- */}
        {activeTab === 'CHARGERS' && (
          <div className="rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl p-6 sm:p-8 shadow-xl">
            <div className="flex items-center justify-between mb-6">
              <div>
                <h3 className="text-lg font-bold text-white">Charger Safety & Verification Queue</h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  Inspect electrical ratings and certify hosts before stations appear on public driver maps.
                </p>
              </div>
              <button
                onClick={fetchChargers}
                className="text-xs text-cyan-400 hover:text-white"
              >
                Refresh Queue
              </button>
            </div>

            {loadingChargers ? (
              <div className="py-12 text-center text-slate-400 text-xs">
                <div className="inline-block w-6 h-6 border-2 border-cyan-400 border-t-transparent rounded-full animate-spin mb-2" />
                <p>Loading stations...</p>
              </div>
            ) : chargers.length === 0 ? (
              <div className="p-8 rounded-2xl bg-white/[0.02] border border-white/10 text-center text-slate-400 text-xs">
                No chargers found in queue.
              </div>
            ) : (
              <div className="space-y-3">
                {chargers.map((c) => {
                  const isPending = c.verificationStatus === 'PENDING';
                  const isApproved = c.verificationStatus === 'APPROVED';

                  return (
                    <div
                      key={c.id}
                      className="p-4 rounded-2xl bg-white/[0.02] border border-white/[0.06] hover:border-white/15 transition-all flex flex-col md:flex-row md:items-center justify-between gap-4 text-xs"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-sm text-white">{c.title}</span>
                          <span
                            className={`px-2 py-0.5 rounded-full text-[10px] font-semibold border ${
                              isApproved
                                ? 'bg-emerald-500/10 text-emerald-300 border-emerald-500/30'
                                : isPending
                                ? 'bg-amber-500/10 text-amber-300 border-amber-500/30'
                                : 'bg-rose-500/10 text-rose-300 border-rose-500/30'
                            }`}
                          >
                            {c.verificationStatus}
                          </span>
                        </div>
                        <div className="text-slate-400">
                          {c.addressLine}, {c.city} • Host ID: {c.hostId}
                        </div>
                        <div className="text-slate-300 font-mono">
                          {c.maxPowerKw} kW ({c.powerType}) • ₹{c.pricePerKwh}/kWh • Sockets: {c.connectorTypes?.join(', ')}
                        </div>
                        {c.rejectionReason && (
                          <div className="text-rose-400 italic">Rejection note: {c.rejectionReason}</div>
                        )}
                      </div>

                      {/* Verification Actions */}
                      <div className="flex items-center gap-2 shrink-0">
                        {!isApproved && (
                          <button
                            onClick={() => handleVerifyCharger(c.id, 'APPROVE')}
                            className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs shadow-md shadow-emerald-600/30 transition-all flex items-center gap-1.5"
                          >
                            <Check className="w-3.5 h-3.5" />
                            Approve
                          </button>
                        )}
                        {c.verificationStatus !== 'REJECTED' && (
                          <button
                            onClick={() => setRejectingCharger(c)}
                            className="px-3 py-2 rounded-xl bg-white/[0.04] hover:bg-rose-500/10 text-slate-400 hover:text-rose-300 font-medium text-xs transition-colors"
                          >
                            Reject
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* ----------------- TAB 3: PAYOUT APPROVALS ----------------- */}
        {activeTab === 'PAYOUTS' && (
          <div className="rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl p-6 sm:p-8 shadow-xl">
            <div className="flex items-center justify-between mb-6">
              <div>
                <h3 className="text-lg font-bold text-white">Host Payout Disbursal Queue</h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  Approve and disburse accumulated charging earnings to verified host UPI IDs and bank accounts.
                </p>
              </div>
              <button
                onClick={fetchPayouts}
                className="text-xs text-cyan-400 hover:text-white"
              >
                Refresh Payouts
              </button>
            </div>

            {loadingPayouts ? (
              <div className="py-12 text-center text-slate-400 text-xs">
                <div className="inline-block w-6 h-6 border-2 border-emerald-400 border-t-transparent rounded-full animate-spin mb-2" />
                <p>Loading payouts...</p>
              </div>
            ) : payouts.length === 0 ? (
              <div className="p-8 rounded-2xl bg-white/[0.02] border border-white/10 text-center text-slate-400 text-xs">
                No payout requests in ledger.
              </div>
            ) : (
              <div className="space-y-3">
                {payouts.map((p) => {
                  const isPending = p.status === 'PENDING';
                  return (
                    <div
                      key={p.id}
                      className="p-4 rounded-2xl bg-white/[0.02] border border-white/[0.06] hover:border-white/15 transition-all flex flex-col md:flex-row md:items-center justify-between gap-4 text-xs"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-cyan-300 font-bold">{p.payoutReference}</span>
                          <span className="text-white font-semibold">({p.hostName || `Host #${p.hostId}`})</span>
                          <span
                            className={`px-2 py-0.5 rounded-full text-[10px] font-semibold border ${
                              p.status === 'PROCESSED'
                                ? 'bg-emerald-500/10 text-emerald-300 border-emerald-500/30'
                                : p.status === 'PENDING'
                                ? 'bg-amber-500/10 text-amber-300 border-amber-500/30'
                                : 'bg-rose-500/10 text-rose-300 border-rose-500/30'
                            }`}
                          >
                            {p.status}
                          </span>
                        </div>
                        <div className="text-slate-400">
                          Transfer via <strong>{p.payoutMethod}</strong> • Account: <code className="text-slate-300">{p.accountDetails}</code>
                        </div>
                        {p.transactionReference && (
                          <div className="text-emerald-400 font-mono text-[11px]">UTR: {p.transactionReference}</div>
                        )}
                        {p.failureReason && (
                          <div className="text-rose-400 italic text-[11px]">Reason: {p.failureReason}</div>
                        )}
                      </div>

                      {/* Right: Amount & Action */}
                      <div className="flex items-center gap-4 shrink-0">
                        <div className="text-right">
                          <div className="text-xs text-slate-400">Amount</div>
                          <div className="text-xl font-extrabold text-white font-mono">₹{p.amount}</div>
                        </div>

                        {isPending && (
                          <div className="flex items-center gap-2">
                            <button
                              onClick={() => {
                                setProcessingPayout(p);
                                setTransactionRef(`UTR-${Date.now()}`);
                              }}
                              className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs shadow-md shadow-emerald-600/30 transition-all flex items-center gap-1.5"
                            >
                              <Check className="w-3.5 h-3.5" />
                              Process
                            </button>
                            <button
                              onClick={() => setRejectingPayout(p)}
                              className="px-3 py-2 rounded-xl bg-white/[0.04] hover:bg-rose-500/10 text-slate-400 hover:text-rose-300 text-xs font-medium transition-colors"
                            >
                              Reject
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* ----------------- TAB 4: USERS DIRECTORY ----------------- */}
        {activeTab === 'USERS' && (
          <div className="rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl p-6 sm:p-8 shadow-xl">
            <div className="flex items-center justify-between mb-6">
              <div>
                <h3 className="text-lg font-bold text-white">Platform User Directory</h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  Manage user accounts, roles, security permissions, and suspension controls.
                </p>
              </div>
              <button
                onClick={fetchUsers}
                className="text-xs text-cyan-400 hover:text-white"
              >
                Refresh Users
              </button>
            </div>

            {loadingUsers ? (
              <div className="py-12 text-center text-slate-400 text-xs">
                <div className="inline-block w-6 h-6 border-2 border-cyan-400 border-t-transparent rounded-full animate-spin mb-2" />
                <p>Loading user directory...</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-white/[0.08] text-slate-400 uppercase tracking-wider text-[11px]">
                      <th className="pb-3 font-semibold">User</th>
                      <th className="pb-3 font-semibold">Email</th>
                      <th className="pb-3 font-semibold">Roles</th>
                      <th className="pb-3 font-semibold">Status</th>
                      <th className="pb-3 font-semibold text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/[0.04]">
                    {users.map((u) => {
                      const isActive = u.status === 'ACTIVE';
                      return (
                        <tr key={u.id} className="hover:bg-white/[0.02] transition-colors">
                          <td className="py-3.5 font-semibold text-white">
                            {u.fullName}
                          </td>
                          <td className="py-3.5 text-slate-400 font-mono">
                            {u.email}
                          </td>
                          <td className="py-3.5">
                            <div className="flex gap-1">
                              {u.roles?.map((r) => (
                                <span
                                  key={r}
                                  className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-white/[0.06] text-cyan-300"
                                >
                                  {r.replace('ROLE_', '')}
                                </span>
                              ))}
                            </div>
                          </td>
                          <td className="py-3.5">
                            <span
                              className={`px-2.5 py-0.5 rounded-full text-[10px] font-semibold border ${
                                isActive
                                  ? 'bg-emerald-500/10 text-emerald-300 border-emerald-500/30'
                                  : 'bg-rose-500/10 text-rose-300 border-rose-500/30'
                              }`}
                            >
                              {u.status}
                            </span>
                          </td>
                          <td className="py-3.5 text-right">
                            <button
                              onClick={() => handleToggleUserStatus(u.id, u.status)}
                              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                                isActive
                                  ? 'bg-white/[0.04] text-slate-400 hover:text-rose-400 hover:bg-rose-500/10'
                                  : 'bg-emerald-500/20 text-emerald-300 hover:bg-emerald-500/30'
                              }`}
                            >
                              {isActive ? 'Suspend' : 'Reactivate'}
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Reject Charger Modal */}
      {rejectingCharger && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md">
          <div className="relative w-full max-w-md rounded-3xl border border-white/[0.12] bg-[#071328] p-6 shadow-2xl text-white">
            <h3 className="text-xl font-bold mb-2">Reject Charger Verification</h3>
            <p className="text-xs text-slate-400 mb-4">
              Specify reason for rejecting <strong>{rejectingCharger.title}</strong>:
            </p>
            <textarea
              rows="3"
              placeholder="e.g. Electrical grounding inspection failed, incorrect socket photo..."
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-white text-xs mb-4 focus:outline-none focus:border-rose-400"
            />
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setRejectingCharger(null)}
                className="px-4 py-2 text-slate-400 hover:text-white text-xs"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => handleVerifyCharger(rejectingCharger.id, 'REJECT', rejectionReason)}
                className="px-5 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-semibold text-xs shadow-md shadow-rose-600/30"
              >
                Confirm Rejection
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Process Payout Modal */}
      {processingPayout && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md">
          <div className="relative w-full max-w-md rounded-3xl border border-white/[0.12] bg-[#071328] p-6 shadow-2xl text-white">
            <h3 className="text-xl font-bold mb-2">Process Payout Disbursement</h3>
            <p className="text-xs text-slate-400 mb-4">
              Disbursing <strong className="text-emerald-400 font-mono">₹{processingPayout.amount}</strong> to{' '}
              {processingPayout.accountDetails} ({processingPayout.payoutMethod}).
            </p>
            <form onSubmit={handleProcessPayout} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">
                  Bank UTR / Transaction Reference *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. UTR928374829102"
                  value={transactionRef}
                  onChange={(e) => setTransactionRef(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-white font-mono text-xs focus:outline-none focus:border-emerald-400"
                />
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setProcessingPayout(null)}
                  className="px-4 py-2 text-slate-400 hover:text-white text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs shadow-md shadow-emerald-600/30"
                >
                  Confirm & Release Funds
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Reject Payout Modal */}
      {rejectingPayout && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md">
          <div className="relative w-full max-w-md rounded-3xl border border-white/[0.12] bg-[#071328] p-6 shadow-2xl text-white">
            <h3 className="text-xl font-bold mb-2">Reject Payout Request</h3>
            <p className="text-xs text-slate-400 mb-4">
              Rejecting withdrawal of <strong className="text-white font-mono">₹{rejectingPayout.amount}</strong> for{' '}
              {rejectingPayout.accountDetails}.
            </p>
            <form onSubmit={handleRejectPayout} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">
                  Rejection Reason *
                </label>
                <textarea
                  rows="3"
                  required
                  placeholder="e.g. Invalid UPI VPA, Bank account IFSC inactive..."
                  value={payoutRejectReason}
                  onChange={(e) => setPayoutRejectReason(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-white text-xs focus:outline-none focus:border-rose-400"
                />
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setRejectingPayout(null)}
                  className="px-4 py-2 text-slate-400 hover:text-white text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-semibold text-xs shadow-md shadow-rose-600/30"
                >
                  Reject Payout
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
