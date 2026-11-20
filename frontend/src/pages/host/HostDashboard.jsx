import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client.js';
import {
  Zap,
  Plus,
  Trash2,
  Power,
  Clock,
  MapPin,
  QrCode,
  ShieldAlert,
  ShieldCheck,
  AlertCircle,
  CheckCircle2,
  X,
  Sparkles,
  Users,
  Eye,
  Calendar,
  AlertTriangle,
  StopCircle,
} from 'lucide-react';

const CONNECTOR_OPTIONS = [
  { id: 'CCS2', label: 'CCS Type 2 (DC Fast)' },
  { id: 'TYPE_2', label: 'Type 2 Mennekes (AC)' },
  { id: 'GB_T', label: 'GB/T (India EV Standard)' },
  { id: 'CHADEMO', label: 'CHAdeMO (DC Fast)' },
  { id: 'TYPE_1', label: 'Type 1 J1772 (AC)' },
];

const PRESET_CHARGERS = [
  {
    title: 'Indiranagar Home Wallbox',
    description: 'Residential 7.2 kW Type-2 wallbox in private driveway with 24/7 CCTV surveillance.',
    locationType: 'HOME',
    addressLine: '12th Main Road, HAL 2nd Stage',
    city: 'Bengaluru',
    state: 'Karnataka',
    postalCode: '560038',
    country: 'India',
    latitude: 12.9716,
    longitude: 77.5946,
    powerType: 'AC',
    maxPowerKw: 7.2,
    pricePerKwh: 12.5,
    connectorTypes: ['TYPE_2', 'CCS2'],
  },
  {
    title: 'MG Road Commercial Fast Hub',
    description: 'Ultra fast 60 kW DC charger with dual CCS2 guns, dedicated security guard and waiting lounge.',
    locationType: 'COMMERCIAL',
    addressLine: '45 MG Road, Near Metro Station',
    city: 'Bengaluru',
    state: 'Karnataka',
    postalCode: '560001',
    country: 'India',
    latitude: 12.975,
    longitude: 77.608,
    powerType: 'DC',
    maxPowerKw: 60.0,
    pricePerKwh: 18.5,
    connectorTypes: ['CCS2'],
  },
  {
    title: 'Koramangala Office Campus Charger',
    description: '22 kW three-phase AC fast charger in covered corporate parking with cafe nearby.',
    locationType: 'OFFICE',
    addressLine: '80 Feet Road, 4th Block',
    city: 'Bengaluru',
    state: 'Karnataka',
    postalCode: '560034',
    country: 'India',
    latitude: 12.9352,
    longitude: 77.6245,
    powerType: 'AC',
    maxPowerKw: 22.0,
    pricePerKwh: 14.0,
    connectorTypes: ['TYPE_2', 'CCS2'],
  },
];

export const HostDashboard = () => {
  const [chargers, setChargers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Register Modal State
  const [isRegisterOpen, setIsRegisterOpen] = useState(false);
  const [submittingCharger, setSubmittingCharger] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    locationType: 'HOME',
    addressLine: '',
    city: 'Bengaluru',
    state: 'Karnataka',
    postalCode: '560038',
    country: 'India',
    latitude: 12.9716,
    longitude: 77.5946,
    powerType: 'AC',
    maxPowerKw: 7.2,
    pricePerKwh: 14.0,
    connectorTypes: ['TYPE_2', 'CCS2'],
  });

  // Station Detail Modal (Bookings & Sessions)
  const [selectedChargerDetails, setSelectedChargerDetails] = useState(null);
  const [chargerBookings, setChargerBookings] = useState([]);
  const [chargerSessions, setChargerSessions] = useState([]);
  const [loadingDetails, setLoadingDetails] = useState(false);

  // QR Code Modal
  const [qrModalCharger, setQrModalCharger] = useState(null);

  const fetchChargers = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await api.get('/api/host/chargers');
      setChargers(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to load your host chargers.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchChargers();
  }, []);

  const handleApplyPreset = (p) => {
    setFormData({
      ...p,
      connectorTypes: [...p.connectorTypes],
    });
  };

  const handleToggleConnector = (type) => {
    setFormData((prev) => {
      const exists = prev.connectorTypes.includes(type);
      const updated = exists
        ? prev.connectorTypes.filter((c) => c !== type)
        : [...prev.connectorTypes, type];
      return { ...prev, connectorTypes: updated };
    });
  };

  const handleCreateCharger = async (e) => {
    e.preventDefault();
    if (formData.connectorTypes.length === 0) {
      setError('Please select at least one supported connector type.');
      return;
    }

    try {
      setSubmittingCharger(true);
      setError('');
      const payload = {
        title: formData.title.trim(),
        description: formData.description.trim() || undefined,
        locationType: formData.locationType,
        addressLine: formData.addressLine.trim(),
        city: formData.city.trim(),
        state: formData.state.trim(),
        postalCode: formData.postalCode.trim(),
        country: formData.country.trim(),
        latitude: parseFloat(formData.latitude),
        longitude: parseFloat(formData.longitude),
        powerType: formData.powerType,
        maxPowerKw: parseFloat(formData.maxPowerKw),
        pricePerKwh: parseFloat(formData.pricePerKwh),
        connectorTypes: formData.connectorTypes,
      };

      await api.post('/api/host/chargers', payload);
      setSuccess(`Charger "${formData.title}" registered! Pending Admin approval.`);
      setIsRegisterOpen(false);
      await fetchChargers();
    } catch (err) {
      setError(err.message || 'Failed to register charger.');
    } finally {
      setSubmittingCharger(false);
    }
  };

  const handleToggleAvailability = async (chargerId, currentStatus) => {
    const nextStatus = currentStatus === 'AVAILABLE' ? 'MAINTENANCE' : 'AVAILABLE';
    try {
      setError('');
      await api.patch(`/api/host/chargers/${chargerId}/availability`, {
        availabilityStatus: nextStatus,
      });
      setSuccess(`Station updated to ${nextStatus}.`);
      setChargers((prev) =>
        prev.map((c) => (c.id === chargerId ? { ...c, availabilityStatus: nextStatus } : c))
      );
    } catch (err) {
      setError(err.message || 'Failed to update availability.');
    }
  };

  const handleDeleteCharger = async (id, title) => {
    if (!window.confirm(`Are you sure you want to delete charger "${title}"?`)) return;
    try {
      setError('');
      await api.delete(`/api/host/chargers/${id}`);
      setSuccess('Charger removed successfully.');
      setChargers((prev) => prev.filter((c) => c.id !== id));
    } catch (err) {
      setError(err.message || 'Failed to delete charger.');
    }
  };

  const handleOpenDetails = async (charger) => {
    setSelectedChargerDetails(charger);
    setLoadingDetails(true);
    try {
      const [bookings, sessions] = await Promise.all([
        api.get(`/api/host/chargers/${charger.id}/bookings`),
        api.get(`/api/host/chargers/${charger.id}/sessions`),
      ]);
      setChargerBookings(Array.isArray(bookings) ? bookings : []);
      setChargerSessions(Array.isArray(sessions) ? sessions : []);
    } catch (err) {
      setError('Could not load station reservations and sessions.');
    } finally {
      setLoadingDetails(false);
    }
  };

  const handleEmergencyStop = async (chargerId, sessionId) => {
    if (!window.confirm('Trigger emergency shutdown for this session? Power will be cut immediately.')) return;
    try {
      await api.post(`/api/host/chargers/${chargerId}/sessions/${sessionId}/emergency-stop`, {
        reason: 'HOST_SAFETY_EMERGENCY_STOP',
      });
      setSuccess('Emergency shutdown sent. Session stopped.');
      // Refresh details
      const sessions = await api.get(`/api/host/chargers/${chargerId}/sessions`);
      setChargerSessions(Array.isArray(sessions) ? sessions : []);
    } catch (err) {
      setError(err.message || 'Emergency stop failed.');
    }
  };

  return (
    <div className="relative min-h-[calc(100vh-4.5rem)] bg-[#061224] py-10 px-4 sm:px-6 lg:px-8">
      {/* Background glow meshes */}
      <div className="absolute top-1/4 right-1/4 w-96 h-96 bg-[#7928ca]/15 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 left-1/4 w-96 h-96 bg-[#00d4ff]/10 rounded-full blur-3xl pointer-events-none" />

      <div className="relative max-w-7xl mx-auto space-y-8">
        {/* Header Bar */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-white/[0.08] pb-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-purple-500/10 border border-purple-500/20 text-purple-300 text-xs font-semibold mb-2">
              <Zap className="w-3.5 h-3.5" />
              Host Station Fleet Manager
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
              Host Charging Hub
            </h1>
            <p className="text-slate-400 text-sm mt-1">
              Monetize your private charging sockets, monitor live kilowatt sessions, and set tariff rates.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <Link
              to="/host/earnings"
              className="px-4 py-2.5 rounded-full bg-white/[0.06] hover:bg-white/[0.1] text-white font-semibold text-xs border border-white/10 transition-colors"
            >
              View Wallet & Payouts →
            </Link>
            <button
              onClick={() => setIsRegisterOpen(true)}
              className="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-full bg-gradient-to-r from-[#635bff] to-[#a855f7] text-white font-semibold text-xs shadow-lg shadow-[#635bff]/25 hover:shadow-[#635bff]/40 hover:scale-[1.02] active:scale-[0.98] transition-all"
            >
              <Plus className="w-4 h-4" />
              Register Station
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

        {/* Chargers Fleet Grid */}
        {loading ? (
          <div className="py-20 text-center">
            <div className="inline-block w-8 h-8 border-4 border-purple-400 border-t-transparent rounded-full animate-spin mb-4" />
            <p className="text-slate-400 text-sm">Loading your host charging stations...</p>
          </div>
        ) : chargers.length === 0 ? (
          <div className="rounded-3xl border border-white/[0.08] bg-white/[0.02] backdrop-blur-xl p-12 text-center max-w-xl mx-auto">
            <div className="w-16 h-16 rounded-2xl bg-purple-500/10 border border-purple-500/20 text-purple-400 flex items-center justify-center mx-auto mb-4">
              <Zap className="w-8 h-8" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">No Charging Stations Registered</h3>
            <p className="text-slate-400 text-sm mb-6">
              Turn your EV wallbox, office socket, or commercial charger into recurring passive income.
            </p>
            <button
              onClick={() => setIsRegisterOpen(true)}
              className="inline-flex items-center gap-2 px-6 py-3 rounded-full bg-[#635bff] text-white font-semibold text-sm hover:bg-[#5347e8] transition-all"
            >
              <Plus className="w-4 h-4" />
              Register First Charger
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {chargers.map((c) => {
              const isApproved = c.verificationStatus === 'APPROVED';
              const isAvailable = c.availabilityStatus === 'AVAILABLE';

              return (
                <div
                  key={c.id}
                  className="rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl p-6 hover:border-purple-500/40 hover:bg-white/[0.05] transition-all flex flex-col justify-between"
                >
                  <div>
                    {/* Header: Verification & Availability */}
                    <div className="flex items-center justify-between gap-2 mb-4">
                      {/* Verification Badge */}
                      {isApproved ? (
                        <span className="px-2.5 py-1 rounded-full text-[11px] font-semibold bg-emerald-500/10 text-emerald-300 border border-emerald-500/30 flex items-center gap-1">
                          <ShieldCheck className="w-3 h-3" />
                          Approved
                        </span>
                      ) : c.verificationStatus === 'REJECTED' ? (
                        <span className="px-2.5 py-1 rounded-full text-[11px] font-semibold bg-rose-500/10 text-rose-300 border border-rose-500/30 flex items-center gap-1">
                          <ShieldAlert className="w-3 h-3" />
                          Rejected
                        </span>
                      ) : (
                        <span className="px-2.5 py-1 rounded-full text-[11px] font-semibold bg-amber-500/10 text-amber-300 border border-amber-500/30 flex items-center gap-1">
                          <Clock className="w-3 h-3" />
                          Pending Audit
                        </span>
                      )}

                      {/* Availability Switch */}
                      <button
                        onClick={() => handleToggleAvailability(c.id, c.availabilityStatus)}
                        className={`px-2.5 py-1 rounded-full text-[11px] font-semibold border flex items-center gap-1.5 transition-colors ${
                          isAvailable
                            ? 'bg-emerald-500/10 text-emerald-300 border-emerald-500/30 hover:bg-emerald-500/20'
                            : 'bg-slate-800 text-slate-400 border-slate-700 hover:text-white'
                        }`}
                        title="Click to toggle availability"
                      >
                        <Power className="w-3 h-3" />
                        {c.availabilityStatus || 'AVAILABLE'}
                      </button>
                    </div>

                    {/* Title & Address */}
                    <h3 className="text-lg font-bold text-white leading-snug mb-1">
                      {c.title}
                    </h3>
                    <p className="text-xs text-slate-400 mb-4 flex items-center gap-1">
                      <MapPin className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
                      {c.addressLine}, {c.city}
                    </p>

                    {/* Hardware Specs Box */}
                    <div className="p-3.5 rounded-2xl bg-white/[0.02] border border-white/[0.06] space-y-2 text-xs mb-4">
                      <div className="flex justify-between">
                        <span className="text-slate-400">Power Rating:</span>
                        <span className="font-semibold text-white">
                          {c.maxPowerKw} kW ({c.powerType})
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-400">Tariff Rate:</span>
                        <span className="font-bold text-emerald-400">
                          ₹{c.pricePerKwh} / kWh
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-400">Location Type:</span>
                        <span className="text-slate-300 font-medium">{c.locationType}</span>
                      </div>
                    </div>

                    {/* Connector Ports */}
                    <div className="mb-4">
                      <span className="block text-[11px] uppercase tracking-wider text-slate-500 font-semibold mb-1.5">
                        Installed Sockets:
                      </span>
                      <div className="flex flex-wrap gap-1.5">
                        {c.connectorTypes?.map((conn) => (
                          <span
                            key={conn}
                            className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-purple-500/10 text-purple-300 border border-purple-500/25"
                          >
                            {conn}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>

                  {/* Actions Bar */}
                  <div className="pt-4 border-t border-white/[0.06] flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setQrModalCharger(c)}
                        className="p-2 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-slate-300 hover:text-white transition-colors"
                        title="Display Station QR Code"
                      >
                        <QrCode className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleOpenDetails(c)}
                        className="px-3 py-2 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-cyan-300 text-xs font-semibold transition-colors flex items-center gap-1.5"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        Sessions & Bookings
                      </button>
                    </div>

                    <button
                      onClick={() => handleDeleteCharger(c.id, c.title)}
                      className="p-2 rounded-xl text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                      title="Delete charger"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* QR Code Preview Modal */}
      {qrModalCharger && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md">
          <div className="relative w-full max-w-sm rounded-3xl border border-white/[0.12] bg-[#071328] p-6 shadow-2xl text-center text-white">
            <button
              onClick={() => setQrModalCharger(null)}
              className="absolute top-4 right-4 text-slate-400 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>
            <div className="w-16 h-16 rounded-2xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 flex items-center justify-center mx-auto mb-3">
              <QrCode className="w-8 h-8" />
            </div>
            <h3 className="text-lg font-bold">{qrModalCharger.title}</h3>
            <p className="text-xs text-slate-400 mb-4">{qrModalCharger.addressLine}</p>

            <div className="p-4 rounded-2xl bg-white text-slate-950 font-mono text-xl font-bold tracking-widest border border-white/20 mb-4 select-all shadow-inner">
              {qrModalCharger.qrCode || `CHG-QR-${qrModalCharger.id}`}
            </div>
            <p className="text-[11px] text-slate-400">
              Drivers scan this QR identifier at your station to plug in and start charging.
            </p>
          </div>
        </div>
      )}

      {/* Station Details Modal (Bookings & Sessions) */}
      {selectedChargerDetails && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md overflow-y-auto">
          <div className="relative w-full max-w-3xl rounded-3xl border border-white/[0.12] bg-[#071328] p-6 sm:p-8 shadow-2xl my-8 text-white">
            <button
              onClick={() => setSelectedChargerDetails(null)}
              className="absolute top-6 right-6 p-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/10"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="mb-6">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-cyan-500/10 border border-cyan-500/20 text-cyan-300 text-xs font-semibold mb-2">
                <Users className="w-3.5 h-3.5" />
                Station Telemetry Log
              </div>
              <h2 className="text-2xl font-bold">{selectedChargerDetails.title}</h2>
              <p className="text-slate-400 text-xs mt-1">
                Real-time reservation schedule and charging sessions recorded at this station.
              </p>
            </div>

            {loadingDetails ? (
              <div className="py-12 text-center text-slate-400 text-xs">
                <div className="inline-block w-6 h-6 border-2 border-cyan-400 border-t-transparent rounded-full animate-spin mb-2" />
                <p>Loading station logs...</p>
              </div>
            ) : (
              <div className="space-y-6">
                {/* Active / Past Sessions */}
                <div>
                  <h4 className="text-xs uppercase tracking-wider text-slate-400 font-semibold mb-3 flex items-center justify-between">
                    <span>Charging Sessions ({chargerSessions.length})</span>
                  </h4>
                  {chargerSessions.length === 0 ? (
                    <div className="p-4 rounded-xl bg-white/[0.02] border border-white/10 text-center text-slate-400 text-xs">
                      No charging sessions recorded yet.
                    </div>
                  ) : (
                    <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                      {chargerSessions.map((s) => (
                        <div
                          key={s.id}
                          className="p-3 rounded-xl bg-white/[0.03] border border-white/10 flex items-center justify-between text-xs"
                        >
                          <div>
                            <div className="font-mono font-bold text-white flex items-center gap-2">
                              <span>{s.sessionReference}</span>
                              <span
                                className={`px-2 py-0.5 rounded-full text-[10px] ${
                                  s.status === 'ACTIVE'
                                    ? 'bg-emerald-500/20 text-emerald-300'
                                    : 'bg-slate-700 text-slate-300'
                                }`}
                              >
                                {s.status}
                              </span>
                            </div>
                            <div className="text-slate-400 text-[11px] mt-0.5">
                              Energy: {s.energyDeliveredKwh} kWh • Billed: ₹{s.totalCost}
                            </div>
                          </div>

                          {s.status === 'ACTIVE' && (
                            <button
                              onClick={() => handleEmergencyStop(selectedChargerDetails.id, s.id)}
                              className="px-3 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-500 text-white font-bold text-[11px] flex items-center gap-1 shadow-md shadow-rose-600/30"
                            >
                              <StopCircle className="w-3.5 h-3.5" />
                              Emergency Stop
                            </button>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Bookings */}
                <div>
                  <h4 className="text-xs uppercase tracking-wider text-slate-400 font-semibold mb-3">
                    Driver Reservations ({chargerBookings.length})
                  </h4>
                  {chargerBookings.length === 0 ? (
                    <div className="p-4 rounded-xl bg-white/[0.02] border border-white/10 text-center text-slate-400 text-xs">
                      No reservations currently booked for this station.
                    </div>
                  ) : (
                    <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                      {chargerBookings.map((b) => (
                        <div
                          key={b.id}
                          className="p-3 rounded-xl bg-white/[0.03] border border-white/10 flex items-center justify-between text-xs"
                        >
                          <div>
                            <div className="font-semibold text-white">
                              {b.bookingReference} • {b.driverName || 'EV Driver'}
                            </div>
                            <div className="text-slate-400 text-[11px] mt-0.5 font-mono">
                              {new Date(b.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} -{' '}
                              {new Date(b.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} (₹{b.totalPrice})
                            </div>
                          </div>
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-cyan-500/10 text-cyan-300">
                            {b.status}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Register Station Modal */}
      {isRegisterOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md overflow-y-auto">
          <div className="relative w-full max-w-2xl rounded-3xl border border-white/[0.12] bg-[#071328] p-6 sm:p-8 shadow-2xl my-8 text-white">
            <button
              onClick={() => setIsRegisterOpen(false)}
              className="absolute top-6 right-6 p-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/10"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="mb-6">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-purple-500/20 border border-purple-500/40 text-purple-300 text-xs font-semibold mb-2">
                <Zap className="w-3.5 h-3.5" />
                Add Charger
              </div>
              <h2 className="text-2xl font-bold">Register EV Charging Station</h2>
              <p className="text-slate-400 text-xs mt-1">
                Enter your charger's technical specifications and electrical parameters.
              </p>
            </div>

            {/* Quick Fill Presets */}
            <div className="mb-6">
              <label className="block text-xs uppercase tracking-wider text-slate-400 font-semibold mb-2">
                ⚡ Quick Fill Preset Templates:
              </label>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                {PRESET_CHARGERS.map((p) => (
                  <button
                    key={p.title}
                    type="button"
                    onClick={() => handleApplyPreset(p)}
                    className="text-left p-2.5 rounded-xl border border-white/[0.06] bg-white/[0.02] hover:bg-white/[0.06] hover:border-purple-500/30 transition-all text-xs"
                  >
                    <div className="font-semibold text-white truncate">{p.title}</div>
                    <div className="text-[11px] text-purple-300 font-mono mt-0.5">
                      {p.maxPowerKw} kW • ₹{p.pricePerKwh}/kWh
                    </div>
                  </button>
                ))}
              </div>
            </div>

            {/* Form */}
            <form onSubmit={handleCreateCharger} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Station Title *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Indiranagar Home Wallbox"
                    value={formData.title}
                    onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                    className="w-full px-3.5 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-white text-xs focus:outline-none focus:border-purple-400"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Location Type *</label>
                  <select
                    value={formData.locationType}
                    onChange={(e) => setFormData({ ...formData, locationType: e.target.value })}
                    className="w-full px-3.5 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-white text-xs focus:outline-none focus:border-purple-400"
                  >
                    <option value="HOME" className="bg-slate-900">Residential Home</option>
                    <option value="COMMERCIAL" className="bg-slate-900">Commercial Complex</option>
                    <option value="OFFICE" className="bg-slate-900">Corporate Office</option>
                    <option value="HOTEL" className="bg-slate-900">Hotel & Resort</option>
                    <option value="RESTAURANT" className="bg-slate-900">Restaurant / Cafe</option>
                    <option value="OTHER" className="bg-slate-900">Other</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Address Line *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. 12th Main Road, HAL 2nd Stage"
                  value={formData.addressLine}
                  onChange={(e) => setFormData({ ...formData, addressLine: e.target.value })}
                  className="w-full px-3.5 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-white text-xs focus:outline-none focus:border-purple-400"
                />
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">City *</label>
                  <input
                    type="text"
                    required
                    value={formData.city}
                    onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-white text-xs"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">State *</label>
                  <input
                    type="text"
                    required
                    value={formData.state}
                    onChange={(e) => setFormData({ ...formData, state: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-white text-xs"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Latitude *</label>
                  <input
                    type="number"
                    step="0.0001"
                    required
                    value={formData.latitude}
                    onChange={(e) => setFormData({ ...formData, latitude: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-white font-mono text-xs"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Longitude *</label>
                  <input
                    type="number"
                    step="0.0001"
                    required
                    value={formData.longitude}
                    onChange={(e) => setFormData({ ...formData, longitude: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-white font-mono text-xs"
                  />
                </div>
              </div>

              {/* Power Specs & Price */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Current Type *</label>
                  <select
                    value={formData.powerType}
                    onChange={(e) => setFormData({ ...formData, powerType: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-white text-xs"
                  >
                    <option value="AC" className="bg-slate-900">AC Level 2</option>
                    <option value="DC" className="bg-slate-900">DC Fast Charger</option>
                    <option value="BOTH" className="bg-slate-900">Both AC / DC</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Max Power (kW) *</label>
                  <input
                    type="number"
                    step="0.1"
                    min="1"
                    required
                    value={formData.maxPowerKw}
                    onChange={(e) => setFormData({ ...formData, maxPowerKw: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-white font-mono text-xs"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Tariff (₹ / kWh) *</label>
                  <input
                    type="number"
                    step="0.5"
                    min="1"
                    required
                    value={formData.pricePerKwh}
                    onChange={(e) => setFormData({ ...formData, pricePerKwh: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-emerald-400 font-mono text-xs"
                  />
                </div>
              </div>

              {/* Connectors */}
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-2">
                  Installed Connector Ports *
                </label>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                  {CONNECTOR_OPTIONS.map((c) => {
                    const isSelected = formData.connectorTypes.includes(c.id);
                    return (
                      <button
                        key={c.id}
                        type="button"
                        onClick={() => handleToggleConnector(c.id)}
                        className={`p-2.5 rounded-xl border text-left text-xs transition-all ${
                          isSelected
                            ? 'bg-purple-500/20 border-purple-400 text-purple-200'
                            : 'bg-white/[0.02] border-white/10 text-slate-400'
                        }`}
                      >
                        <div className="font-semibold text-white">{c.label}</div>
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-white/[0.08]">
                <button
                  type="button"
                  onClick={() => setIsRegisterOpen(false)}
                  className="px-4 py-2 text-slate-400 hover:text-white text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingCharger}
                  className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-[#635bff] to-[#a855f7] text-white font-semibold text-xs shadow-lg shadow-[#635bff]/25 hover:scale-[1.01] transition-all flex items-center gap-2"
                >
                  {submittingCharger ? 'Submitting...' : 'Register Station'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
