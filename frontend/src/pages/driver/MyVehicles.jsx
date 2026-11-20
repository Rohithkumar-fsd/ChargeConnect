import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { api } from '../../api/client.js';
import {
  Car,
  Plus,
  Trash2,
  Zap,
  BatteryCharging,
  Compass,
  AlertCircle,
  CheckCircle2,
  X,
  Sparkles,
} from 'lucide-react';

const CONNECTOR_OPTIONS = [
  { id: 'CCS2', label: 'CCS Type 2 (DC Fast)', desc: 'Standard DC fast charging in India & EU' },
  { id: 'TYPE_2', label: 'Type 2 Mennekes (AC)', desc: 'Standard AC wallbox charging' },
  { id: 'GB_T', label: 'GB/T (India EV Standard)', desc: 'Used by fleet & commercial EVs' },
  { id: 'CHADEMO', label: 'CHAdeMO (DC)', desc: 'Japanese DC fast standard' },
  { id: 'TYPE_1', label: 'Type 1 J1772 (AC)', desc: 'North American standard' },
];

const PRESET_EVS = [
  {
    manufacturer: 'Tata',
    model: 'Nexon EV Long Range',
    batteryCapacityKwh: '40.50',
    maxAcPowerKw: '7.20',
    maxDcPowerKw: '50.00',
    connectorTypes: ['CCS2', 'TYPE_2'],
  },
  {
    manufacturer: 'MG',
    model: 'ZS EV Exclusive',
    batteryCapacityKwh: '50.30',
    maxAcPowerKw: '7.40',
    maxDcPowerKw: '50.00',
    connectorTypes: ['CCS2', 'TYPE_2'],
  },
  {
    manufacturer: 'Mahindra',
    model: 'XUV400 EL Pro',
    batteryCapacityKwh: '39.40',
    maxAcPowerKw: '7.20',
    maxDcPowerKw: '50.00',
    connectorTypes: ['CCS2', 'TYPE_2'],
  },
  {
    manufacturer: 'Hyundai',
    model: 'Ioniq 5',
    batteryCapacityKwh: '72.60',
    maxAcPowerKw: '11.00',
    maxDcPowerKw: '350.00',
    connectorTypes: ['CCS2', 'TYPE_2'],
  },
];

export const MyVehicles = () => {
  const navigate = useNavigate();
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    manufacturer: '',
    model: '',
    registrationNumber: '',
    batteryCapacityKwh: '',
    maxAcPowerKw: '7.2',
    maxDcPowerKw: '50.0',
    connectorTypes: ['CCS2', 'TYPE_2'],
  });

  const fetchVehicles = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await api.get('/api/driver/vehicles');
      setVehicles(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to load your vehicles.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchVehicles();
  }, []);

  const handleApplyPreset = (preset) => {
    setFormData((prev) => ({
      ...prev,
      manufacturer: preset.manufacturer,
      model: preset.model,
      batteryCapacityKwh: preset.batteryCapacityKwh,
      maxAcPowerKw: preset.maxAcPowerKw,
      maxDcPowerKw: preset.maxDcPowerKw,
      connectorTypes: [...preset.connectorTypes],
    }));
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

  const handleCreateVehicle = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (formData.connectorTypes.length === 0) {
      setError('Please select at least one supported connector type.');
      return;
    }

    try {
      setSubmitting(true);
      const payload = {
        manufacturer: formData.manufacturer.trim(),
        model: formData.model.trim(),
        registrationNumber: formData.registrationNumber.trim() || undefined,
        batteryCapacityKwh: parseFloat(formData.batteryCapacityKwh),
        maxAcPowerKw: parseFloat(formData.maxAcPowerKw),
        maxDcPowerKw: parseFloat(formData.maxDcPowerKw),
        connectorTypes: formData.connectorTypes,
      };

      await api.post('/api/driver/vehicles', payload);
      setSuccess(`Added ${formData.manufacturer} ${formData.model} successfully!`);
      setIsModalOpen(false);
      // Reset form
      setFormData({
        manufacturer: '',
        model: '',
        registrationNumber: '',
        batteryCapacityKwh: '',
        maxAcPowerKw: '7.2',
        maxDcPowerKw: '50.0',
        connectorTypes: ['CCS2', 'TYPE_2'],
      });
      await fetchVehicles();
    } catch (err) {
      setError(err.message || 'Failed to create vehicle');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteVehicle = async (id, name) => {
    if (!window.confirm(`Are you sure you want to remove ${name}?`)) return;
    try {
      setError('');
      await api.delete(`/api/driver/vehicles/${id}`);
      setSuccess(`Removed vehicle successfully.`);
      setVehicles((prev) => prev.filter((v) => v.id !== id));
    } catch (err) {
      setError(err.message || 'Failed to remove vehicle');
    }
  };

  return (
    <div className="relative min-h-[calc(100vh-4.5rem)] bg-[#061224] py-10 px-4 sm:px-6 lg:px-8">
      {/* Background glow meshes */}
      <div className="absolute top-1/4 left-1/4 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-[#635bff]/15 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-[#00d4ff]/10 rounded-full blur-3xl pointer-events-none" />

      <div className="relative max-w-6xl mx-auto space-y-8">
        {/* Header Bar */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-white/[0.08] pb-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-cyan-500/10 border border-cyan-500/20 text-cyan-300 text-xs font-semibold mb-2">
              <Car className="w-3.5 h-3.5" />
              EV Garage Management
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
              My Electric Vehicles
            </h1>
            <p className="text-slate-400 text-sm mt-1">
              Register your EVs to unlock smart compatibility filtering for nearby private chargers.
            </p>
          </div>

          <button
            onClick={() => setIsModalOpen(true)}
            className="inline-flex items-center justify-center gap-2 px-5 py-3 rounded-full bg-gradient-to-r from-[#635bff] to-[#00d4ff] text-white font-semibold text-sm shadow-lg shadow-[#635bff]/25 hover:shadow-[#635bff]/40 hover:scale-[1.02] active:scale-[0.98] transition-all"
          >
            <Plus className="w-4 h-4" />
            Add New EV
          </button>
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

        {/* Vehicle Grid or Empty State */}
        {loading ? (
          <div className="py-20 text-center">
            <div className="inline-block w-8 h-8 border-4 border-cyan-400 border-t-transparent rounded-full animate-spin mb-4" />
            <p className="text-slate-400">Loading your EV garage...</p>
          </div>
        ) : vehicles.length === 0 ? (
          <div className="rounded-3xl border border-white/[0.08] bg-white/[0.02] backdrop-blur-xl p-12 text-center max-w-xl mx-auto">
            <div className="w-16 h-16 rounded-2xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 flex items-center justify-center mx-auto mb-4">
              <Car className="w-8 h-8" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">No Vehicles Registered Yet</h3>
            <p className="text-slate-400 text-sm mb-6">
              To discover compatible chargers and calculate precise charging times, add your EV model and connector types.
            </p>
            <button
              onClick={() => setIsModalOpen(true)}
              className="inline-flex items-center gap-2 px-6 py-3 rounded-full bg-[#635bff] text-white font-semibold text-sm hover:bg-[#5347e8] transition-all"
            >
              <Plus className="w-4 h-4" />
              Register My First EV
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {vehicles.map((v) => (
              <div
                key={v.id}
                className="group relative rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl p-6 hover:border-cyan-500/40 hover:bg-white/[0.05] transition-all flex flex-col justify-between"
              >
                <div>
                  {/* Top row */}
                  <div className="flex items-start justify-between gap-2 mb-4">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-[#635bff]/20 to-[#00d4ff]/20 border border-white/10 flex items-center justify-center text-cyan-300">
                        <Car className="w-6 h-6" />
                      </div>
                      <div>
                        <span className="text-xs uppercase tracking-wider text-slate-400 font-semibold">
                          {v.manufacturer}
                        </span>
                        <h3 className="text-lg font-bold text-white leading-tight">
                          {v.model}
                        </h3>
                      </div>
                    </div>

                    <button
                      onClick={() => handleDeleteVehicle(v.id, `${v.manufacturer} ${v.model}`)}
                      className="p-2 rounded-xl text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                      title="Delete vehicle"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>

                  {/* Reg Plate */}
                  {v.registrationNumber && (
                    <div className="inline-block px-2.5 py-1 rounded-md bg-white/[0.06] border border-white/10 font-mono text-xs text-slate-300 font-medium mb-4">
                      {v.registrationNumber}
                    </div>
                  )}

                  {/* Specs List */}
                  <div className="space-y-2 py-3 border-y border-white/[0.06] text-xs">
                    <div className="flex justify-between items-center text-slate-400">
                      <span className="flex items-center gap-1.5">
                        <BatteryCharging className="w-3.5 h-3.5 text-emerald-400" />
                        Battery Capacity
                      </span>
                      <span className="font-semibold text-white">{v.batteryCapacityKwh} kWh</span>
                    </div>

                    <div className="flex justify-between items-center text-slate-400">
                      <span className="flex items-center gap-1.5">
                        <Zap className="w-3.5 h-3.5 text-cyan-400" />
                        Max AC Charging
                      </span>
                      <span className="font-semibold text-white">{v.maxAcPowerKw} kW</span>
                    </div>

                    <div className="flex justify-between items-center text-slate-400">
                      <span className="flex items-center gap-1.5">
                        <Zap className="w-3.5 h-3.5 text-violet-400" />
                        Max DC Fast Charging
                      </span>
                      <span className="font-semibold text-white">{v.maxDcPowerKw} kW</span>
                    </div>
                  </div>

                  {/* Connector Badges */}
                  <div className="mt-4">
                    <p className="text-[11px] uppercase tracking-wider text-slate-400 font-semibold mb-2">
                      Compatible Ports:
                    </p>
                    <div className="flex flex-wrap gap-1.5">
                      {v.connectorTypes?.map((c) => (
                        <span
                          key={c}
                          className="px-2.5 py-1 rounded-full text-[11px] font-semibold bg-cyan-500/10 text-cyan-300 border border-cyan-500/25"
                        >
                          {c}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>

                {/* Card Action */}
                <div className="mt-6 pt-4">
                  <button
                    onClick={() => navigate(`/driver/find?vehicleId=${v.id}`)}
                    className="w-full py-2.5 px-4 rounded-xl bg-white/[0.06] hover:bg-gradient-to-r hover:from-[#635bff] hover:to-[#00d4ff] text-slate-200 hover:text-white font-semibold text-xs flex items-center justify-center gap-2 transition-all"
                  >
                    <Compass className="w-4 h-4" />
                    Find Compatible Chargers
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Add Vehicle Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md overflow-y-auto">
          <div className="relative w-full max-w-xl rounded-3xl border border-white/[0.12] bg-[#0b172a] p-6 sm:p-8 shadow-2xl my-8">
            {/* Close Button */}
            <button
              onClick={() => setIsModalOpen(false)}
              className="absolute top-6 right-6 p-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/10 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>

            {/* Modal Header */}
            <div className="mb-6">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#635bff]/20 border border-[#635bff]/40 text-violet-300 text-xs font-semibold mb-2">
                <Car className="w-3.5 h-3.5" />
                Add Vehicle
              </div>
              <h2 className="text-2xl font-bold text-white">Register Electric Vehicle</h2>
              <p className="text-slate-400 text-xs mt-1">
                Pick a popular Indian EV template or manually enter your vehicle specs.
              </p>
            </div>

            {/* Quick Presets */}
            <div className="mb-6">
              <label className="block text-xs uppercase tracking-wider text-slate-400 font-semibold mb-2">
                ⚡ Quick Fill Preset:
              </label>
              <div className="grid grid-cols-2 gap-2">
                {PRESET_EVS.map((p) => (
                  <button
                    key={p.model}
                    type="button"
                    onClick={() => handleApplyPreset(p)}
                    className="text-left p-2.5 rounded-xl border border-white/[0.06] bg-white/[0.02] hover:bg-white/[0.06] hover:border-cyan-500/30 transition-all text-xs"
                  >
                    <div className="font-semibold text-white">{p.manufacturer} {p.model}</div>
                    <div className="text-[11px] text-cyan-300 font-mono mt-0.5">{p.batteryCapacityKwh} kWh • CCS2</div>
                  </button>
                ))}
              </div>
            </div>

            {/* Form */}
            <form onSubmit={handleCreateVehicle} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">
                    Manufacturer *
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Tata, MG, Tesla"
                    value={formData.manufacturer}
                    onChange={(e) => setFormData({ ...formData, manufacturer: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">
                    Model *
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Nexon EV, ZS EV"
                    value={formData.model}
                    onChange={(e) => setFormData({ ...formData, model: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">
                    Reg. Number
                  </label>
                  <input
                    type="text"
                    placeholder="KA01AB1234"
                    value={formData.registrationNumber}
                    onChange={(e) => setFormData({ ...formData, registrationNumber: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">
                    Battery (kWh) *
                  </label>
                  <input
                    type="number"
                    step="0.1"
                    min="1"
                    required
                    placeholder="40.5"
                    value={formData.batteryCapacityKwh}
                    onChange={(e) => setFormData({ ...formData, batteryCapacityKwh: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">
                    Max AC (kW)
                  </label>
                  <input
                    type="number"
                    step="0.1"
                    min="1"
                    required
                    placeholder="7.2"
                    value={formData.maxAcPowerKw}
                    onChange={(e) => setFormData({ ...formData, maxAcPowerKw: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400"
                  />
                </div>
              </div>

              {/* Supported Connector Types */}
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-2">
                  Supported Connector Ports * (Select at least one)
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  {CONNECTOR_OPTIONS.map((c) => {
                    const isSelected = formData.connectorTypes.includes(c.id);
                    return (
                      <button
                        key={c.id}
                        type="button"
                        onClick={() => handleToggleConnector(c.id)}
                        className={`p-3 rounded-xl border text-left transition-all ${
                          isSelected
                            ? 'bg-cyan-500/10 border-cyan-400 text-cyan-200'
                            : 'bg-white/[0.02] border-white/10 text-slate-400 hover:border-white/20'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <span className="font-semibold text-xs text-white">{c.label}</span>
                          {isSelected && <CheckCircle2 className="w-4 h-4 text-cyan-400" />}
                        </div>
                        <p className="text-[10px] text-slate-400 mt-0.5">{c.desc}</p>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Submit Buttons */}
              <div className="flex items-center justify-end gap-3 pt-4 border-t border-white/[0.08]">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2.5 rounded-xl text-slate-400 hover:text-white hover:bg-white/10 text-xs font-semibold transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-[#635bff] to-[#00d4ff] text-white font-semibold text-xs shadow-lg shadow-[#635bff]/25 hover:shadow-[#635bff]/40 disabled:opacity-50 transition-all flex items-center gap-2"
                >
                  {submitting ? (
                    <>
                      <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                      Saving...
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-3.5 h-3.5" />
                      Save Vehicle
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
