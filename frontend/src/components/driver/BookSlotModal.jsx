import React, { useState, useEffect } from 'react';
import { api } from '../../api/client.js';
import {
  Calendar,
  Clock,
  Zap,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  X,
  CreditCard,
  Lock,
  ArrowRight,
  Sparkles,
  Smartphone,
  Check,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const BookSlotModal = ({ charger, vehicle, isOpen, onClose, onBookingSuccess }) => {
  const navigate = useNavigate();

  // Booking Flow Steps: 'SELECT_SLOTS' | 'CHECKOUT' | 'SUCCESS'
  const [step, setStep] = useState('SELECT_SLOTS');

  // Step 1: Slot selection
  const [selectedDate, setSelectedDate] = useState(() => {
    const today = new Date();
    return today.toISOString().split('T')[0];
  });
  const [availableSlots, setAvailableSlots] = useState([]);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [selectedSlotIndex, setSelectedSlotIndex] = useState(null);
  const [slotDurationMinutes, setSlotDurationMinutes] = useState(60); // 30, 60, 90, 120

  // Step 2: Payment & Lock
  const [createdBooking, setCreatedBooking] = useState(null);
  const [paymentOrder, setPaymentOrder] = useState(null);
  const [lockCountdown, setLockCountdown] = useState(600); // 10 minutes in seconds
  const [processingPayment, setProcessingPayment] = useState(false);
  const [selectedPaymentMethod, setSelectedPaymentMethod] = useState('UPI');

  // Error handling
  const [error, setError] = useState('');

  // Fetch available slots when charger or date changes
  useEffect(() => {
    if (!isOpen || !charger) return;

    const fetchSlots = async () => {
      try {
        setLoadingSlots(true);
        setError('');
        setSelectedSlotIndex(null);
        const data = await api.get(
          `/api/driver/bookings/available-slots?chargerId=${charger.id}&date=${selectedDate}`
        );
        setAvailableSlots(data.slots || []);
      } catch (err) {
        setError(err.message || 'Failed to fetch available slots.');
        setAvailableSlots([]);
      } finally {
        setLoadingSlots(false);
      }
    };

    fetchSlots();
  }, [isOpen, charger, selectedDate]);

  // Countdown timer for 10-minute lock during CHECKOUT step
  useEffect(() => {
    let timer = null;
    if (step === 'CHECKOUT' && lockCountdown > 0) {
      timer = setInterval(() => {
        setLockCountdown((prev) => (prev > 0 ? prev - 1 : 0));
      }, 1000);
    }
    return () => {
      if (timer) clearInterval(timer);
    };
  }, [step, lockCountdown]);

  if (!isOpen || !charger) return null;

  // Selected time window calculation
  let startTime = null;
  let endTime = null;
  if (selectedSlotIndex !== null && availableSlots[selectedSlotIndex]) {
    const startIso = availableSlots[selectedSlotIndex].startTime;
    const startObj = new Date(startIso);
    const endObj = new Date(startObj.getTime() + slotDurationMinutes * 60 * 1000);
    startTime = startObj.toISOString();
    endTime = endObj.toISOString();
  }

  // Cost calculation
  const hours = slotDurationMinutes / 60;
  const powerKw = charger.maxPowerKw || 7.2;
  const estimatedKwh = Math.min(
    powerKw * hours,
    vehicle?.batteryCapacityKwh ? parseFloat(vehicle.batteryCapacityKwh) : 50.0
  );
  const pricePerKwh = charger.pricePerKwh || 15.0;
  const energyCost = Math.round(estimatedKwh * pricePerKwh * 100) / 100;
  const platformFee = 15.0;
  const totalAmount = (energyCost + platformFee).toFixed(2);

  // Format lock timer as MM:SS
  const formatTimer = (secs) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  // STEP 1 -> STEP 2: Lock booking in Redis & Create Razorpay Order
  const handleProceedToPayment = async () => {
    if (!startTime || !endTime) {
      setError('Please select a start time slot.');
      return;
    }
    if (!vehicle?.id) {
      setError('Please choose a vehicle first.');
      return;
    }

    try {
      setError('');
      setProcessingPayment(true);

      // 1. Create booking (triggers Redis checkout lock)
      const bookingPayload = {
        chargerId: charger.id,
        vehicleId: vehicle.id,
        startTime: startTime,
        endTime: endTime,
      };

      const booking = await api.post('/api/driver/bookings', bookingPayload);
      setCreatedBooking(booking);

      // 2. Create payment order
      const order = await api.post('/api/driver/payments/create-order', {
        bookingId: booking.id,
      });
      setPaymentOrder(order);

      // Reset lock timer to 10 minutes
      setLockCountdown(600);
      setStep('CHECKOUT');
    } catch (err) {
      setError(err.message || 'Failed to hold booking slot.');
    } finally {
      setProcessingPayment(false);
    }
  };

  // STEP 2 -> STEP 3: Complete Payment Verification
  const handleConfirmPayment = async () => {
    if (!createdBooking || !paymentOrder) return;

    try {
      setError('');
      setProcessingPayment(true);

      const verifyPayload = {
        bookingId: createdBooking.id,
        razorpayOrderId: paymentOrder.razorpayOrderId,
        razorpayPaymentId: `pay_mock_${Date.now()}`,
        razorpaySignature: 'mock_signature', // Accepted by backend dev gateway
      };

      await api.post('/api/driver/payments/verify', verifyPayload);

      setStep('SUCCESS');
      if (onBookingSuccess) {
        onBookingSuccess(createdBooking);
      }
    } catch (err) {
      setError(err.message || 'Payment verification failed.');
    } finally {
      setProcessingPayment(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md overflow-y-auto">
      <div className="relative w-full max-w-2xl rounded-3xl border border-white/[0.12] bg-[#071328] p-6 sm:p-8 shadow-2xl my-8 text-white">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-6 right-6 p-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/10 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        {/* ----------------- STEP 1: SELECT SLOTS ----------------- */}
        {step === 'SELECT_SLOTS' && (
          <div>
            <div className="mb-6">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-cyan-500/10 border border-cyan-500/20 text-cyan-300 text-xs font-semibold mb-2">
                <Calendar className="w-3.5 h-3.5" />
                Reserve Charging Slot
              </div>
              <h2 className="text-2xl font-bold tracking-tight">{charger.title}</h2>
              <p className="text-slate-400 text-xs mt-1">
                {charger.addressLine}, {charger.city} • ₹{charger.pricePerKwh}/kWh • {charger.maxPowerKw} kW
              </p>
            </div>

            {error && (
              <div className="mb-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {/* Date Selector */}
            <div className="mb-4">
              <label className="block text-xs uppercase tracking-wider text-slate-400 font-semibold mb-2">
                Select Date:
              </label>
              <div className="flex gap-2">
                <input
                  type="date"
                  value={selectedDate}
                  min={new Date().toISOString().split('T')[0]}
                  onChange={(e) => setSelectedDate(e.target.value)}
                  className="px-4 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-white text-sm focus:outline-none focus:border-cyan-400"
                />
                <button
                  type="button"
                  onClick={() => setSelectedDate(new Date().toISOString().split('T')[0])}
                  className="px-3 py-2 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-xs font-medium text-slate-300 transition-colors"
                >
                  Today
                </button>
                <button
                  type="button"
                  onClick={() => {
                    const d = new Date();
                    d.setDate(d.getDate() + 1);
                    setSelectedDate(d.toISOString().split('T')[0]);
                  }}
                  className="px-3 py-2 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-xs font-medium text-slate-300 transition-colors"
                >
                  Tomorrow
                </button>
              </div>
            </div>

            {/* Duration Selector */}
            <div className="mb-4">
              <label className="block text-xs uppercase tracking-wider text-slate-400 font-semibold mb-2">
                Session Duration:
              </label>
              <div className="grid grid-cols-4 gap-2">
                {[30, 60, 90, 120].map((mins) => (
                  <button
                    key={mins}
                    type="button"
                    onClick={() => setSlotDurationMinutes(mins)}
                    className={`py-2 px-3 rounded-xl text-xs font-semibold border transition-all ${
                      slotDurationMinutes === mins
                        ? 'bg-cyan-500/20 border-cyan-400 text-cyan-300 shadow-md shadow-cyan-500/10'
                        : 'bg-white/[0.03] border-white/10 text-slate-400 hover:text-white hover:bg-white/[0.06]'
                    }`}
                  >
                    {mins >= 60 ? `${mins / 60} hr` : `${mins} min`}
                  </button>
                ))}
              </div>
            </div>

            {/* Time Slot Grid */}
            <div className="mb-6">
              <label className="block text-xs uppercase tracking-wider text-slate-400 font-semibold mb-2 flex items-center justify-between">
                <span>Pick Start Time:</span>
                <span className="text-[11px] text-slate-500 font-normal">
                  (Green = available, Dim/Strikethrough = past or booked)
                </span>
              </label>

              {loadingSlots ? (
                <div className="py-8 text-center text-slate-400 text-xs">
                  <div className="inline-block w-6 h-6 border-2 border-cyan-400 border-t-transparent rounded-full animate-spin mb-2" />
                  <p>Checking available slots...</p>
                </div>
              ) : availableSlots.length === 0 ? (
                <div className="p-4 rounded-xl bg-white/[0.02] border border-white/10 text-center text-slate-400 text-xs">
                  No slots found for this date.
                </div>
              ) : (
                <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-2 max-h-48 overflow-y-auto pr-1">
                  {availableSlots.map((slot, idx) => {
                    const isPast = new Date(slot.startTime).getTime() <= Date.now();
                    const isAvailable = slot.status === 'AVAILABLE' && !isPast;
                    const isSelected = selectedSlotIndex === idx;
                    const timeLabel = new Date(slot.startTime).toLocaleTimeString([], {
                      hour: '2-digit',
                      minute: '2-digit',
                      hour12: true,
                    });

                    return (
                      <button
                        key={idx}
                        type="button"
                        disabled={!isAvailable}
                        onClick={() => setSelectedSlotIndex(idx)}
                        title={isPast ? 'This time slot has already passed' : isAvailable ? 'Available' : 'Booked'}
                        className={`p-2 rounded-xl text-center text-xs font-mono transition-all border ${
                          isSelected
                            ? 'bg-gradient-to-r from-[#635bff] to-[#00d4ff] text-white border-transparent font-bold shadow-lg shadow-[#635bff]/30'
                            : isAvailable
                            ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300 hover:bg-emerald-500/20'
                            : 'bg-white/[0.02] border-white/[0.05] text-slate-600 line-through cursor-not-allowed'
                        }`}
                      >
                        {timeLabel}
                      </button>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Summary & Price preview */}
            {selectedSlotIndex !== null && (
              <div className="p-4 rounded-2xl bg-white/[0.04] border border-white/10 mb-6 space-y-2 text-xs">
                <div className="flex justify-between text-slate-300">
                  <span>Vehicle:</span>
                  <span className="font-semibold text-white">
                    {vehicle?.manufacturer} {vehicle?.model} ({vehicle?.batteryCapacityKwh} kWh)
                  </span>
                </div>
                <div className="flex justify-between text-slate-300">
                  <span>Reserved Window:</span>
                  <span className="font-semibold text-cyan-300 font-mono">
                    {new Date(startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} -{' '}
                    {new Date(endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} ({hours} hr)
                  </span>
                </div>
                <div className="flex justify-between text-slate-300">
                  <span>Estimated Energy:</span>
                  <span className="font-semibold text-white">{estimatedKwh.toFixed(1)} kWh</span>
                </div>
                <div className="flex justify-between text-slate-300">
                  <span>Energy Cost (@ ₹{pricePerKwh}/kWh):</span>
                  <span>₹{energyCost.toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-slate-300">
                  <span>Platform & Grid Access Fee:</span>
                  <span>₹{platformFee.toFixed(2)}</span>
                </div>
                <div className="pt-2 border-t border-white/10 flex justify-between text-sm font-bold text-white">
                  <span>Total Payable:</span>
                  <span className="text-emerald-400">₹{totalAmount}</span>
                </div>
              </div>
            )}

            {/* Action Bar */}
            <div className="flex items-center justify-end gap-3 pt-4 border-t border-white/[0.08]">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2.5 rounded-xl text-slate-400 hover:text-white text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={selectedSlotIndex === null || processingPayment}
                onClick={handleProceedToPayment}
                className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-[#635bff] to-[#00d4ff] text-white font-semibold text-xs shadow-lg shadow-[#635bff]/25 hover:shadow-[#635bff]/40 disabled:opacity-50 transition-all flex items-center gap-2"
              >
                {processingPayment ? (
                  <>
                    <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    Locking Slot in Redis...
                  </>
                ) : (
                  <>
                    <Lock className="w-3.5 h-3.5" />
                    Hold Slot & Proceed to Pay
                  </>
                )}
              </button>
            </div>
          </div>
        )}

        {/* ----------------- STEP 2: CHECKOUT (STRIPE/RAZORPAY STYLE) ----------------- */}
        {step === 'CHECKOUT' && (
          <div>
            <div className="mb-6">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-xs font-semibold mb-2">
                <ShieldCheck className="w-3.5 h-3.5" />
                Redis Slot Lock Active
              </div>
              <h2 className="text-2xl font-bold tracking-tight">Complete Payment</h2>
              <p className="text-slate-400 text-xs mt-1">
                Your charger slot is exclusively reserved for you for the next{' '}
                <span className="text-cyan-400 font-mono font-bold">{formatTimer(lockCountdown)}</span>.
              </p>
            </div>

            {error && (
              <div className="mb-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {/* Payment Summary Box */}
            <div className="p-5 rounded-2xl bg-gradient-to-br from-[#635bff]/10 via-[#071328] to-[#00d4ff]/10 border border-white/10 mb-6">
              <div className="flex justify-between items-center mb-3">
                <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold">
                  Order Total
                </span>
                <span className="text-2xl font-extrabold text-white">₹{totalAmount}</span>
              </div>
              <div className="space-y-1 text-xs text-slate-300">
                <div className="flex justify-between">
                  <span>Order Reference:</span>
                  <span className="font-mono text-cyan-300">{paymentOrder?.razorpayOrderId}</span>
                </div>
                <div className="flex justify-between">
                  <span>Station:</span>
                  <span className="text-white font-medium">{charger.title}</span>
                </div>
                <div className="flex justify-between">
                  <span>Lock Expiry:</span>
                  <span className="font-mono text-amber-400 font-semibold">
                    {formatTimer(lockCountdown)} remaining
                  </span>
                </div>
              </div>
            </div>

            {/* Payment Method Selector */}
            <div className="space-y-3 mb-6">
              <label className="block text-xs uppercase tracking-wider text-slate-400 font-semibold">
                Select Payment Method:
              </label>

              <button
                type="button"
                onClick={() => setSelectedPaymentMethod('UPI')}
                className={`w-full p-3.5 rounded-xl border text-left flex items-center justify-between transition-all ${
                  selectedPaymentMethod === 'UPI'
                    ? 'bg-cyan-500/10 border-cyan-400 text-white'
                    : 'bg-white/[0.02] border-white/10 text-slate-400 hover:border-white/20'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
                    <Smartphone className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-white">Instant UPI (GPay / PhonePe / Paytm)</div>
                    <div className="text-[11px] text-slate-400">Zero surcharge • Direct bank deduction</div>
                  </div>
                </div>
                {selectedPaymentMethod === 'UPI' && <Check className="w-4 h-4 text-cyan-400" />}
              </button>

              <button
                type="button"
                onClick={() => setSelectedPaymentMethod('CARD')}
                className={`w-full p-3.5 rounded-xl border text-left flex items-center justify-between transition-all ${
                  selectedPaymentMethod === 'CARD'
                    ? 'bg-cyan-500/10 border-cyan-400 text-white'
                    : 'bg-white/[0.02] border-white/10 text-slate-400 hover:border-white/20'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-[#635bff]/20 text-[#635bff] flex items-center justify-center">
                    <CreditCard className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-white">Debit / Credit Card</div>
                    <div className="text-[11px] text-slate-400">Visa, Mastercard, RuPay, Amex</div>
                  </div>
                </div>
                {selectedPaymentMethod === 'CARD' && <Check className="w-4 h-4 text-cyan-400" />}
              </button>
            </div>

            {/* Authorize & Pay button */}
            <div className="space-y-2">
              <button
                type="button"
                disabled={processingPayment || lockCountdown === 0}
                onClick={handleConfirmPayment}
                className="w-full py-3.5 px-6 rounded-xl bg-gradient-to-r from-[#635bff] via-[#7928ca] to-[#00d4ff] text-white font-bold text-sm shadow-xl shadow-[#635bff]/30 hover:scale-[1.01] active:scale-[0.99] disabled:opacity-50 transition-all flex items-center justify-center gap-2"
              >
                {processingPayment ? (
                  <>
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    Verifying Payment Signature...
                  </>
                ) : (
                  <>
                    <Sparkles className="w-4 h-4" />
                    Authorize & Pay ₹{totalAmount}
                  </>
                )}
              </button>
              <p className="text-center text-[10px] text-slate-500">
                Secured by Razorpay • 256-Bit SSL Encryption • P2P Escrow Protection
              </p>
            </div>
          </div>
        )}

        {/* ----------------- STEP 3: SUCCESS ----------------- */}
        {step === 'SUCCESS' && (
          <div className="text-center py-6">
            <div className="w-16 h-16 rounded-full bg-emerald-500/20 border border-emerald-500/40 text-emerald-400 flex items-center justify-center mx-auto mb-4 animate-bounce">
              <CheckCircle2 className="w-10 h-10" />
            </div>

            <h2 className="text-2xl font-bold text-white mb-1">Booking Confirmed!</h2>
            <p className="text-slate-400 text-xs mb-6">
              Your charging slot has been locked and confirmed on the network.
            </p>

            <div className="max-w-md mx-auto p-4 rounded-2xl bg-white/[0.03] border border-white/10 text-left space-y-2 text-xs mb-6">
              <div className="flex justify-between">
                <span className="text-slate-400">Booking Ref:</span>
                <span className="font-mono text-cyan-300 font-bold">
                  {createdBooking?.bookingReference || 'BK-SUCCESS'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Charger:</span>
                <span className="text-white font-semibold">{charger.title}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Time Window:</span>
                <span className="text-white font-mono">
                  {new Date(startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} -{' '}
                  {new Date(endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Amount Paid:</span>
                <span className="text-emerald-400 font-bold">₹{totalAmount}</span>
              </div>
            </div>

            <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
              <button
                type="button"
                onClick={() => {
                  onClose();
                  navigate('/driver/bookings');
                }}
                className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-white/[0.08] hover:bg-white/[0.12] text-white text-xs font-semibold transition-colors"
              >
                View in My Bookings
              </button>
              <button
                type="button"
                onClick={() => {
                  onClose();
                  navigate(`/driver/session?bookingId=${createdBooking?.id}`);
                }}
                className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-gradient-to-r from-emerald-500 to-cyan-500 text-white text-xs font-bold shadow-lg shadow-emerald-500/20 hover:scale-[1.02] transition-all flex items-center justify-center gap-2"
              >
                <Zap className="w-3.5 h-3.5" />
                Go to Live Charging Screen
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
