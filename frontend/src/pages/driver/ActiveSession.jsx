import React, { useState, useEffect, useRef } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { api } from '../../api/client.js';
import {
  Zap,
  BatteryCharging,
  QrCode,
  StopCircle,
  Clock,
  Gauge,
  CheckCircle2,
  AlertCircle,
  Star,
  Receipt,
  Car,
  ShieldCheck,
  Sparkles,
} from 'lucide-react';

const QUICK_QR_PRESETS = [
  { code: 'CHG-QR-2', name: 'MG Road Fast EV Hub (60 kW DC)' },
  { code: 'CHG-QR-1', name: 'Indiranagar Home Wallbox (7.2 kW AC)' },
  { code: 'CHG-QR-3', name: 'Kiran Bengaluru Fast Charger (22 kW)' },
  { code: 'CHG-QR-4', name: 'Indiranagar Fast Hub (22 kW AC)' },
];

export const ActiveSession = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  // Session states
  const [activeSession, setActiveSession] = useState(null);
  const [telemetry, setTelemetry] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Start Session Form (When no active session)
  const [confirmedBookings, setConfirmedBookings] = useState([]);
  const [selectedBookingId, setSelectedBookingId] = useState(() => searchParams.get('bookingId') || '');
  const [qrCodeInput, setQrCodeInput] = useState('CHG-QR-2');
  const [startingSession, setStartingSession] = useState(false);

  // Stop Session
  const [stoppingSession, setStoppingSession] = useState(false);
  const [completedSummary, setCompletedSummary] = useState(null);

  // Review after session stop
  const [isReviewOpen, setIsReviewOpen] = useState(false);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);

  // Interval reference
  const pollingRef = useRef(null);

  // 1. Initial Load: Check active session & confirmed bookings
  const loadInitialData = async () => {
    try {
      setLoading(true);
      setError('');

      // Check active session
      try {
        const session = await api.get('/api/driver/sessions/active');
        if (session && session.id && session.status === 'ACTIVE') {
          setActiveSession(session);
        } else {
          setActiveSession(null);
        }
      } catch {
        setActiveSession(null);
      }

      // Check confirmed bookings to populate dropdown
      try {
        const bookings = await api.get('/api/driver/bookings');
        const confirmed = (Array.isArray(bookings) ? bookings : []).filter(
          (b) => b.status === 'CONFIRMED'
        );
        setConfirmedBookings(confirmed);
        if (!selectedBookingId && confirmed.length > 0) {
          setSelectedBookingId(confirmed[0].id.toString());
        }
      } catch {
        setConfirmedBookings([]);
      }
    } catch (err) {
      setError(err.message || 'Failed to initialize session status.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadInitialData();
  }, []);

  // 2. Telemetry Polling (every 3 seconds when session is active)
  useEffect(() => {
    if (!activeSession?.id) {
      if (pollingRef.current) clearInterval(pollingRef.current);
      setTelemetry(null);
      return;
    }

    const fetchTelemetry = async () => {
      try {
        const data = await api.get(`/api/driver/sessions/${activeSession.id}/telemetry`);
        setTelemetry(data);
      } catch (err) {
        // If 404 or stopped elsewhere
        if (err.status === 404) {
          setActiveSession(null);
        }
      }
    };

    fetchTelemetry();
    pollingRef.current = setInterval(fetchTelemetry, 3000);

    return () => {
      if (pollingRef.current) clearInterval(pollingRef.current);
    };
  }, [activeSession?.id]);

  // Start Session handler
  const handleStartSession = async (e) => {
    e.preventDefault();
    if (!qrCodeInput.trim()) {
      setError('Please provide or scan a charger QR code.');
      return;
    }

    try {
      setStartingSession(true);
      setError('');
      setCompletedSummary(null);

      const payload = {
        qrCode: qrCodeInput.trim(),
        bookingId: selectedBookingId ? parseInt(selectedBookingId) : undefined,
      };

      const session = await api.post('/api/driver/sessions/start', payload);
      setActiveSession(session);
      setSuccess('Plugged in! Electric charging session initiated successfully.');
    } catch (err) {
      setError(err.message || 'Failed to initiate charging session.');
    } finally {
      setStartingSession(false);
    }
  };

  // Stop Session handler
  const handleStopSession = async () => {
    if (!activeSession?.id) return;
    if (!window.confirm('Are you sure you want to stop charging?')) return;

    try {
      setStoppingSession(true);
      setError('');

      const stopped = await api.post(`/api/driver/sessions/${activeSession.id}/stop`, {
        reason: 'DRIVER_MANUAL_STOP',
      });

      setCompletedSummary(stopped);
      setActiveSession(null);
      setTelemetry(null);
      setSuccess('Charging completed! Invoice generated.');
      setIsReviewOpen(true);
    } catch (err) {
      setError(err.message || 'Failed to stop session.');
    } finally {
      setStoppingSession(false);
    }
  };

  // Review submission
  const handleSubmitReview = async (e) => {
    e.preventDefault();
    if (!completedSummary?.id) return;

    try {
      setSubmittingReview(true);
      await api.post('/api/driver/reviews', {
        sessionId: completedSummary.id,
        rating: reviewRating,
        comment: reviewComment,
      });
      setIsReviewOpen(false);
      setSuccess('Thank you! Your verified rating was submitted to the network.');
    } catch (err) {
      setError(err.message || 'Could not submit review.');
    } finally {
      setSubmittingReview(false);
    }
  };

  // Format seconds to HH:MM:SS
  const formatDuration = (totalSeconds) => {
    if (!totalSeconds) return '00:00';
    const hrs = Math.floor(totalSeconds / 3600);
    const mins = Math.floor((totalSeconds % 3600) / 60);
    const secs = totalSeconds % 60;
    if (hrs > 0) {
      return `${hrs.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="relative min-h-[calc(100vh-4.5rem)] bg-[#061224] py-10 px-4 sm:px-6 lg:px-8">
      {/* Background glow meshes */}
      <div className="absolute top-1/4 left-1/3 w-96 h-96 bg-[#00d4ff]/15 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 right-1/3 w-96 h-96 bg-[#10b981]/10 rounded-full blur-3xl pointer-events-none" />

      <div className="relative max-w-4xl mx-auto space-y-8">
        {/* Header Bar */}
        <div className="border-b border-white/[0.08] pb-6 text-center sm:text-left">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-xs font-semibold mb-2">
            <BatteryCharging className="w-3.5 h-3.5 animate-pulse" />
            Live Hardware Telemetry
          </div>
          <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
            EV Charging Cockpit
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Real-time kilowatt delivery, state of charge monitoring, and live rupee billing.
          </p>
        </div>

        {/* Notifications */}
        {error && (
          <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm flex items-center justify-between">
            <div className="flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
            <button onClick={() => setError('')} className="text-rose-400 hover:text-white">
              ✕
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
              ✕
            </button>
          </div>
        )}

        {/* ---------------- STATE 1: ACTIVE CHARGING SESSION ---------------- */}
        {activeSession ? (
          <div className="space-y-6">
            {/* Top Status Card */}
            <div className="rounded-3xl border border-emerald-500/30 bg-gradient-to-b from-emerald-950/20 via-[#071328] to-[#071328] backdrop-blur-xl p-8 shadow-2xl relative overflow-hidden">
              <div className="flex flex-col md:flex-row items-center justify-between gap-8">
                {/* Circular Neon Battery Gauge */}
                <div className="relative flex items-center justify-center shrink-0">
                  <div className="w-56 h-56 rounded-full border-4 border-emerald-500/20 flex items-center justify-center relative">
                    {/* Pulsing ring */}
                    <div className="absolute inset-0 rounded-full border-4 border-emerald-400 border-t-transparent animate-spin" />
                    <div className="absolute inset-2 rounded-full bg-emerald-500/5 animate-pulse" />

                    <div className="text-center z-10">
                      <Zap className="w-8 h-8 text-emerald-400 mx-auto mb-1 animate-bounce" />
                      <div className="text-4xl font-extrabold text-white font-mono">
                        {telemetry?.energyDeliveredKwh !== undefined
                          ? `${telemetry.energyDeliveredKwh.toFixed(2)}`
                          : '0.00'}
                      </div>
                      <div className="text-xs uppercase tracking-widest text-emerald-300 font-bold">
                        kWh Delivered
                      </div>
                      {telemetry?.estimatedBatteryPercent !== null &&
                        telemetry?.estimatedBatteryPercent !== undefined && (
                          <div className="text-[11px] text-cyan-300 font-semibold mt-1">
                            +{telemetry.estimatedBatteryPercent.toFixed(1)}% Added
                          </div>
                        )}
                    </div>
                  </div>
                </div>

                {/* Live Metrics Grid */}
                <div className="flex-1 w-full space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold">
                        Station Active
                      </span>
                      <h2 className="text-xl font-bold text-white">
                        {activeSession.chargerTitle || 'Charging Station'}
                      </h2>
                    </div>
                    <span className="px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 animate-pulse">
                      ⚡ Power Flowing
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-3 text-xs">
                    <div className="p-3.5 rounded-2xl bg-white/[0.04] border border-white/10">
                      <div className="flex items-center gap-1.5 text-slate-400 mb-1">
                        <Gauge className="w-3.5 h-3.5 text-cyan-400" />
                        Live Power
                      </div>
                      <div className="text-xl font-bold text-white font-mono">
                        {telemetry?.currentPowerKw || activeSession.maxPowerKw || 22.0} kW
                      </div>
                    </div>

                    <div className="p-3.5 rounded-2xl bg-white/[0.04] border border-white/10">
                      <div className="flex items-center gap-1.5 text-slate-400 mb-1">
                        <Clock className="w-3.5 h-3.5 text-violet-400" />
                        Elapsed Time
                      </div>
                      <div className="text-xl font-bold text-white font-mono">
                        {formatDuration(telemetry?.elapsedSeconds || 0)}
                      </div>
                    </div>

                    <div className="p-3.5 rounded-2xl bg-white/[0.04] border border-white/10">
                      <div className="flex items-center gap-1.5 text-slate-400 mb-1">
                        <Receipt className="w-3.5 h-3.5 text-amber-400" />
                        Current Cost
                      </div>
                      <div className="text-xl font-bold text-emerald-400 font-mono">
                        ₹{telemetry?.currentCost !== undefined ? telemetry.currentCost.toFixed(2) : '0.00'}
                      </div>
                    </div>

                    <div className="p-3.5 rounded-2xl bg-white/[0.04] border border-white/10">
                      <div className="flex items-center gap-1.5 text-slate-400 mb-1">
                        <Zap className="w-3.5 h-3.5 text-emerald-400" />
                        Tariff Rate
                      </div>
                      <div className="text-xl font-bold text-white font-mono">
                        ₹{telemetry?.pricePerKwh || activeSession.pricePerKwh || '15.00'}/kWh
                      </div>
                    </div>
                  </div>

                  <div className="pt-2">
                    <button
                      onClick={handleStopSession}
                      disabled={stoppingSession}
                      className="w-full py-3.5 px-6 rounded-2xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-sm shadow-xl shadow-rose-600/30 active:scale-[0.98] transition-all flex items-center justify-center gap-2"
                    >
                      {stoppingSession ? (
                        <>
                          <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                          Finalizing Telemetry Meter...
                        </>
                      ) : (
                        <>
                          <StopCircle className="w-5 h-5" />
                          Stop Charging & Generate Invoice
                        </>
                      )}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        ) : (
          /* ---------------- STATE 2: NO ACTIVE SESSION (START CHARGER) ---------------- */
          <div className="space-y-6">
            {/* Completed summary from just stopped session */}
            {completedSummary && (
              <div className="rounded-3xl border border-emerald-500/30 bg-emerald-950/20 backdrop-blur-xl p-6 text-white space-y-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
                    <CheckCircle2 className="w-6 h-6" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold">Session Completed Successfully</h3>
                    <p className="text-xs text-slate-400 font-mono">Ref: {completedSummary.sessionReference}</p>
                  </div>
                </div>

                <div className="grid grid-cols-3 gap-3 text-xs border-y border-white/10 py-3">
                  <div>
                    <span className="text-slate-400">Total Energy:</span>
                    <div className="text-base font-bold text-white font-mono">
                      {completedSummary.energyDeliveredKwh} kWh
                    </div>
                  </div>
                  <div>
                    <span className="text-slate-400">Total Billed:</span>
                    <div className="text-base font-bold text-emerald-400 font-mono">
                      ₹{completedSummary.totalCost}
                    </div>
                  </div>
                  <div>
                    <span className="text-slate-400">Station:</span>
                    <div className="text-sm font-semibold text-white truncate">
                      {completedSummary.chargerTitle}
                    </div>
                  </div>
                </div>

                <button
                  onClick={() => setIsReviewOpen(true)}
                  className="inline-flex items-center gap-2 text-xs font-semibold text-amber-400 hover:text-amber-300"
                >
                  <Star className="w-4 h-4" />
                  Rate your charging host now →
                </button>
              </div>
            )}

            {/* Start Charger Form Card */}
            <div className="rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl p-6 sm:p-8 shadow-xl">
              <div className="mb-6">
                <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#635bff]/20 border border-[#635bff]/40 text-violet-300 text-xs font-semibold mb-2">
                  <QrCode className="w-3.5 h-3.5" />
                  Scan & Plug In
                </div>
                <h2 className="text-2xl font-bold text-white">Start a Charging Session</h2>
                <p className="text-slate-400 text-xs mt-1">
                  Arrived at the station? Scan or enter the charger's QR identifier code to initiate power delivery.
                </p>
              </div>

              {/* Quick QR Fill Buttons */}
              <div className="mb-6">
                <label className="block text-xs uppercase tracking-wider text-slate-400 font-semibold mb-2">
                  ⚡ Test Stations in Bangalore:
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  {QUICK_QR_PRESETS.map((p) => (
                    <button
                      key={p.code}
                      type="button"
                      onClick={() => setQrCodeInput(p.code)}
                      className={`text-left p-2.5 rounded-xl border text-xs transition-all ${
                        qrCodeInput === p.code
                          ? 'bg-cyan-500/10 border-cyan-400 text-cyan-200'
                          : 'bg-white/[0.02] border-white/10 text-slate-400 hover:border-white/20'
                      }`}
                    >
                      <div className="font-mono font-bold text-white">{p.code}</div>
                      <div className="text-[11px] text-slate-400 mt-0.5">{p.name}</div>
                    </button>
                  ))}
                </div>
              </div>

              <form onSubmit={handleStartSession} className="space-y-4">
                {/* Confirmed Booking Link (Optional/Recommended) */}
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1 flex items-center justify-between">
                    <span>Link Confirmed Booking (Optional):</span>
                    <Link to="/driver/bookings" className="text-cyan-400 text-[11px] hover:underline">
                      View Bookings
                    </Link>
                  </label>
                  <select
                    value={selectedBookingId}
                    onChange={(e) => setSelectedBookingId(e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-white text-xs focus:outline-none focus:border-cyan-400"
                  >
                    <option value="" className="bg-slate-900 text-slate-400">
                      -- Walk-in / Direct QR Activation (No prior booking) --
                    </option>
                    {confirmedBookings.map((b) => (
                      <option key={b.id} value={b.id} className="bg-slate-900 text-white">
                        {b.bookingReference} - {b.chargerTitle} (₹{b.totalPrice})
                      </option>
                    ))}
                  </select>
                </div>

                {/* QR Code Input */}
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">
                    Charger QR Identifier *
                  </label>
                  <div className="relative">
                    <input
                      type="text"
                      required
                      placeholder="e.g. CHG-QR-1, CHG-QR-2"
                      value={qrCodeInput}
                      onChange={(e) => setQrCodeInput(e.target.value)}
                      className="w-full px-3.5 py-3 rounded-xl bg-white/[0.04] border border-white/10 text-white font-mono text-sm tracking-wide uppercase focus:outline-none focus:border-cyan-400 pl-10"
                    />
                    <QrCode className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
                  </div>
                </div>

                {/* Start Button */}
                <div className="pt-2">
                  <button
                    type="submit"
                    disabled={startingSession}
                    className="w-full py-3.5 px-6 rounded-2xl bg-gradient-to-r from-emerald-500 via-teal-500 to-cyan-500 text-white font-bold text-sm shadow-xl shadow-emerald-500/25 hover:scale-[1.01] active:scale-[0.99] disabled:opacity-50 transition-all flex items-center justify-center gap-2"
                  >
                    {startingSession ? (
                      <>
                        <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                        Handshaking with Charger Hardware...
                      </>
                    ) : (
                      <>
                        <Zap className="w-4 h-4" />
                        Plug In & Start Charging Now
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>

      {/* Post-Session Review Modal */}
      {isReviewOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md">
          <div className="relative w-full max-w-md rounded-3xl border border-white/[0.12] bg-[#071328] p-6 shadow-2xl text-white">
            <h3 className="text-xl font-bold mb-2">Rate Your Experience</h3>
            <p className="text-xs text-slate-400 mb-4">
              Your feedback ensures high quality and safety standards across the peer-to-peer charging network.
            </p>

            <form onSubmit={handleSubmitReview} className="space-y-4">
              <div>
                <label className="block text-xs uppercase tracking-wider text-slate-400 font-semibold mb-2">
                  Star Rating:
                </label>
                <div className="flex items-center gap-2">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      type="button"
                      onClick={() => setReviewRating(star)}
                      className="p-1 text-2xl transition-transform hover:scale-110"
                    >
                      <Star
                        className={`w-7 h-7 ${
                          star <= reviewRating
                            ? 'text-amber-400 fill-amber-400'
                            : 'text-slate-600'
                        }`}
                      />
                    </button>
                  ))}
                  <span className="text-sm font-bold text-amber-400 ml-2">
                    {reviewRating} of 5 Stars
                  </span>
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">
                  Comments (Optional):
                </label>
                <textarea
                  rows="3"
                  placeholder="e.g. Great charging speed, easy to park, friendly host!"
                  value={reviewComment}
                  onChange={(e) => setReviewComment(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-white text-xs focus:outline-none focus:border-cyan-400"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsReviewOpen(false)}
                  className="px-4 py-2 rounded-xl text-slate-400 hover:text-white text-xs"
                >
                  Skip
                </button>
                <button
                  type="submit"
                  disabled={submittingReview}
                  className="px-5 py-2 rounded-xl bg-gradient-to-r from-[#635bff] to-[#00d4ff] text-white font-semibold text-xs shadow-lg shadow-[#635bff]/25 transition-all flex items-center gap-2"
                >
                  {submittingReview ? 'Submitting...' : 'Submit Rating'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
