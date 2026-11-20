import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { api } from '../../api/client.js';
import {
  Calendar,
  Clock,
  Zap,
  CheckCircle2,
  AlertCircle,
  X,
  CreditCard,
  Ban,
  Star,
  Receipt,
  ArrowRight,
  Sparkles,
} from 'lucide-react';

export const MyBookings = () => {
  const navigate = useNavigate();
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL'); // ALL | CONFIRMED | COMPLETED | CANCELLED
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Cancellation Modal
  const [cancellingBooking, setCancellingBooking] = useState(null);
  const [cancellationReason, setCancellationReason] = useState('');
  const [submittingCancel, setSubmittingCancel] = useState(false);

  // Review Modal
  const [reviewBooking, setReviewBooking] = useState(null);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);

  const fetchBookings = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await api.get('/api/driver/bookings');
      setBookings(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to fetch your bookings.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings();
  }, []);

  const handleCancelBooking = async (e) => {
    e.preventDefault();
    if (!cancellingBooking) return;

    try {
      setSubmittingCancel(true);
      setError('');
      await api.post(`/api/driver/bookings/${cancellingBooking.id}/cancel`, {
        reason: cancellationReason || 'Driver requested cancellation',
      });
      setSuccess(`Booking #${cancellingBooking.bookingReference} cancelled. Refund initiated!`);
      setCancellingBooking(null);
      setCancellationReason('');
      await fetchBookings();
    } catch (err) {
      setError(err.message || 'Failed to cancel booking.');
    } finally {
      setSubmittingCancel(false);
    }
  };

  const handleSubmitReview = async (e) => {
    e.preventDefault();
    if (!reviewBooking) return;

    try {
      setSubmittingReview(true);
      setError('');
      // In ChargeConnect, reviews can be linked via session or booking
      // If session exists, we submit via driver reviews
      await api.post('/api/driver/reviews', {
        sessionId: reviewBooking.sessionId || reviewBooking.id, // backend review takes sessionId
        rating: reviewRating,
        comment: reviewComment,
      });
      setSuccess('Review submitted! Thank you for rating this host charger.');
      setReviewBooking(null);
      setReviewComment('');
    } catch (err) {
      setError(err.message || 'Could not submit review.');
    } finally {
      setSubmittingReview(false);
    }
  };

  const filteredBookings = bookings.filter((b) => {
    if (filter === 'ALL') return true;
    return b.status === filter;
  });

  const confirmedCount = bookings.filter((b) => b.status === 'CONFIRMED').length;
  const completedCount = bookings.filter((b) => b.status === 'COMPLETED').length;
  const totalSpent = bookings
    .filter((b) => b.status === 'CONFIRMED' || b.status === 'COMPLETED')
    .reduce((sum, b) => sum + (parseFloat(b.totalPrice) || 0), 0)
    .toFixed(2);

  const getStatusBadge = (status) => {
    switch (status) {
      case 'CONFIRMED':
        return (
          <span className="px-2.5 py-1 rounded-full text-[11px] font-semibold bg-emerald-500/10 text-emerald-300 border border-emerald-500/30">
            Confirmed
          </span>
        );
      case 'COMPLETED':
        return (
          <span className="px-2.5 py-1 rounded-full text-[11px] font-semibold bg-cyan-500/10 text-cyan-300 border border-cyan-500/30">
            Completed
          </span>
        );
      case 'PENDING':
        return (
          <span className="px-2.5 py-1 rounded-full text-[11px] font-semibold bg-amber-500/10 text-amber-300 border border-amber-500/30">
            Pending Hold
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="px-2.5 py-1 rounded-full text-[11px] font-semibold bg-rose-500/10 text-rose-300 border border-rose-500/30">
            Cancelled
          </span>
        );
      default:
        return (
          <span className="px-2.5 py-1 rounded-full text-[11px] font-semibold bg-slate-500/10 text-slate-400 border border-slate-500/30">
            {status}
          </span>
        );
    }
  };

  return (
    <div className="relative min-h-[calc(100vh-4.5rem)] bg-[#061224] py-10 px-4 sm:px-6 lg:px-8">
      {/* Background glow meshes */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-[#635bff]/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-[#00d4ff]/10 rounded-full blur-3xl pointer-events-none" />

      <div className="relative max-w-6xl mx-auto space-y-8">
        {/* Header Bar */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-white/[0.08] pb-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-cyan-500/10 border border-cyan-500/20 text-cyan-300 text-xs font-semibold mb-2">
              <Calendar className="w-3.5 h-3.5" />
              Reservation History
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
              My EV Bookings
            </h1>
            <p className="text-slate-400 text-sm mt-1">
              Manage your confirmed charging slots, view receipts, and initiate charging sessions.
            </p>
          </div>

          <Link
            to="/driver/find"
            className="inline-flex items-center justify-center gap-2 px-5 py-3 rounded-full bg-gradient-to-r from-[#635bff] to-[#00d4ff] text-white font-semibold text-sm shadow-lg shadow-[#635bff]/25 hover:shadow-[#635bff]/40 hover:scale-[1.02] active:scale-[0.98] transition-all"
          >
            <Zap className="w-4 h-4" />
            Book New Station
          </Link>
        </div>

        {/* Stats Strip */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="p-4 rounded-2xl bg-white/[0.03] border border-white/[0.08] backdrop-blur-xl">
            <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold">Total Bookings</span>
            <div className="text-2xl font-extrabold text-white mt-1">{bookings.length}</div>
          </div>
          <div className="p-4 rounded-2xl bg-white/[0.03] border border-white/[0.08] backdrop-blur-xl">
            <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold">Upcoming</span>
            <div className="text-2xl font-extrabold text-emerald-400 mt-1">{confirmedCount}</div>
          </div>
          <div className="p-4 rounded-2xl bg-white/[0.03] border border-white/[0.08] backdrop-blur-xl">
            <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold">Completed</span>
            <div className="text-2xl font-extrabold text-cyan-300 mt-1">{completedCount}</div>
          </div>
          <div className="p-4 rounded-2xl bg-white/[0.03] border border-white/[0.08] backdrop-blur-xl">
            <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold">Total Spent</span>
            <div className="text-2xl font-extrabold text-white mt-1">₹{totalSpent}</div>
          </div>
        </div>

        {/* Filter Pills */}
        <div className="flex gap-2 border-b border-white/[0.06] pb-3 overflow-x-auto">
          {['ALL', 'CONFIRMED', 'COMPLETED', 'PENDING', 'CANCELLED'].map((cat) => (
            <button
              key={cat}
              onClick={() => setFilter(cat)}
              className={`px-4 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all ${
                filter === cat
                  ? 'bg-white/10 text-white shadow-inner'
                  : 'text-slate-400 hover:text-white hover:bg-white/[0.04]'
              }`}
            >
              {cat.charAt(0) + cat.slice(1).toLowerCase()}
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

        {/* Bookings List */}
        {loading ? (
          <div className="py-20 text-center">
            <div className="inline-block w-8 h-8 border-4 border-cyan-400 border-t-transparent rounded-full animate-spin mb-4" />
            <p className="text-slate-400 text-sm">Fetching reservations...</p>
          </div>
        ) : filteredBookings.length === 0 ? (
          <div className="rounded-3xl border border-white/[0.08] bg-white/[0.02] backdrop-blur-xl p-12 text-center max-w-lg mx-auto">
            <Calendar className="w-12 h-12 text-slate-600 mx-auto mb-3" />
            <h3 className="text-lg font-bold text-white mb-1">No Bookings Found</h3>
            <p className="text-slate-400 text-xs mb-6">
              You haven't reserved any charging slots in this category yet.
            </p>
            <Link
              to="/driver/find"
              className="inline-flex items-center gap-2 px-6 py-2.5 rounded-full bg-[#635bff] text-white text-xs font-semibold hover:bg-[#5347e8] transition-all"
            >
              Explore Nearby Chargers
            </Link>
          </div>
        ) : (
          <div className="space-y-4">
            {filteredBookings.map((b) => {
              const startDate = new Date(b.startTime);
              const endDate = new Date(b.endTime);

              return (
                <div
                  key={b.id}
                  className="rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl p-6 hover:border-white/[0.15] transition-all flex flex-col md:flex-row md:items-center justify-between gap-6"
                >
                  <div className="space-y-2">
                    <div className="flex items-center gap-3">
                      <span className="font-mono text-xs font-semibold text-cyan-300 bg-cyan-500/10 px-2.5 py-0.5 rounded-md border border-cyan-500/20">
                        {b.bookingReference || `BK-${b.id}`}
                      </span>
                      {getStatusBadge(b.status)}
                    </div>

                    <h3 className="text-lg font-bold text-white leading-snug">
                      {b.chargerTitle || 'EV Charging Station'}
                    </h3>

                    <div className="flex flex-wrap items-center gap-y-1 gap-x-4 text-xs text-slate-400">
                      <span className="flex items-center gap-1">
                        <Calendar className="w-3.5 h-3.5 text-slate-400" />
                        {startDate.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })}
                      </span>
                      <span className="flex items-center gap-1 font-mono text-slate-300">
                        <Clock className="w-3.5 h-3.5 text-slate-400" />
                        {startDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} -{' '}
                        {endDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} ({b.totalHours} hrs)
                      </span>
                      <span>•</span>
                      <span>Vehicle: <strong className="text-white">{b.vehicleModel || 'Electric Vehicle'}</strong></span>
                    </div>

                    {b.cancellationReason && (
                      <p className="text-xs text-rose-400/90 italic">
                        Reason: {b.cancellationReason}
                      </p>
                    )}
                  </div>

                  {/* Right side: Price & Actions */}
                  <div className="flex flex-col md:items-end justify-between gap-3 shrink-0">
                    <div className="text-left md:text-right">
                      <div className="text-xs text-slate-400">Total Price</div>
                      <div className="text-xl font-extrabold text-white">₹{b.totalPrice}</div>
                    </div>

                    <div className="flex items-center gap-2">
                      {/* If CONFIRMED, show Start Session & Cancel buttons */}
                      {b.status === 'CONFIRMED' && (
                        <>
                          <button
                            onClick={() => setCancellingBooking(b)}
                            className="px-3.5 py-2 rounded-xl bg-white/[0.04] hover:bg-rose-500/10 hover:text-rose-300 text-slate-400 text-xs font-medium transition-colors"
                          >
                            Cancel
                          </button>
                          <button
                            onClick={() => navigate(`/driver/session?bookingId=${b.id}`)}
                            className="px-4 py-2 rounded-xl bg-gradient-to-r from-emerald-500 to-cyan-500 text-white font-semibold text-xs shadow-lg shadow-emerald-500/20 hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center gap-1.5"
                          >
                            <Zap className="w-3.5 h-3.5" />
                            Start Charging
                          </button>
                        </>
                      )}

                      {/* If COMPLETED, show Write Review */}
                      {b.status === 'COMPLETED' && (
                        <button
                          onClick={() => setReviewBooking(b)}
                          className="px-4 py-2 rounded-xl bg-white/[0.06] hover:bg-white/[0.1] text-cyan-300 hover:text-white font-semibold text-xs transition-colors flex items-center gap-1.5"
                        >
                          <Star className="w-3.5 h-3.5 text-amber-400" />
                          Rate Host
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Cancel Booking Modal */}
      {cancellingBooking && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md">
          <div className="relative w-full max-w-md rounded-3xl border border-white/[0.12] bg-[#071328] p-6 shadow-2xl text-white">
            <h3 className="text-xl font-bold mb-2">Cancel Reservation</h3>
            <p className="text-xs text-slate-400 mb-4">
              Cancelling booking{' '}
              <strong className="text-cyan-300 font-mono">{cancellingBooking.bookingReference}</strong> for{' '}
              {cancellingBooking.chargerTitle}. Any paid amount will be automatically refunded via payment gateway.
            </p>

            <form onSubmit={handleCancelBooking} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">
                  Reason for cancellation:
                </label>
                <textarea
                  rows="3"
                  required
                  placeholder="e.g. Schedule changed, found alternative station..."
                  value={cancellationReason}
                  onChange={(e) => setCancellationReason(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-white text-xs focus:outline-none focus:border-rose-400"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setCancellingBooking(null)}
                  className="px-4 py-2 rounded-xl text-slate-400 hover:text-white text-xs"
                >
                  Keep Reservation
                </button>
                <button
                  type="submit"
                  disabled={submittingCancel}
                  className="px-5 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-semibold text-xs shadow-lg shadow-rose-600/30 transition-all flex items-center gap-2"
                >
                  {submittingCancel ? 'Cancelling...' : 'Confirm Cancellation'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Review Modal */}
      {reviewBooking && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md">
          <div className="relative w-full max-w-md rounded-3xl border border-white/[0.12] bg-[#071328] p-6 shadow-2xl text-white">
            <h3 className="text-xl font-bold mb-2">Rate Your Experience</h3>
            <p className="text-xs text-slate-400 mb-4">
              How was your charging session at{' '}
              <strong className="text-white">{reviewBooking.chargerTitle}</strong>?
            </p>

            <form onSubmit={handleSubmitReview} className="space-y-4">
              <div>
                <label className="block text-xs uppercase tracking-wider text-slate-400 font-semibold mb-2">
                  Rating:
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
                  placeholder="e.g. Great charger speed, easy to park, hospitable host!"
                  value={reviewComment}
                  onChange={(e) => setReviewComment(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-white text-xs focus:outline-none focus:border-cyan-400"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setReviewBooking(null)}
                  className="px-4 py-2 rounded-xl text-slate-400 hover:text-white text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingReview}
                  className="px-5 py-2 rounded-xl bg-gradient-to-r from-[#635bff] to-[#00d4ff] text-white font-semibold text-xs shadow-lg shadow-[#635bff]/25 transition-all flex items-center gap-2"
                >
                  {submittingReview ? 'Submitting...' : 'Submit Review'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
