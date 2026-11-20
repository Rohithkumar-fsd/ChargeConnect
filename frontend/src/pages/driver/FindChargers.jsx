import React, { useState, useEffect } from 'react';
import { useSearchParams, Link, useNavigate } from 'react-router-dom';
import { api } from '../../api/client.js';
import { BookSlotModal } from '../../components/driver/BookSlotModal.jsx';
import {
  Compass,
  MapPin,
  Car,
  Zap,
  Sliders,
  Search,
  Crosshair,
  AlertCircle,
  Clock,
  Sparkles,
  ShieldCheck,
  ChevronRight,
  Filter,
} from 'lucide-react';

const BANGALORE_PRESETS = [
  { name: 'Bangalore Central', lat: 12.9716, lng: 77.5946 },
  { name: 'Indiranagar 100ft Rd', lat: 12.9784, lng: 77.6408 },
  { name: 'MG Road Metro', lat: 12.9750, lng: 77.6080 },
  { name: 'Koramangala 4th Block', lat: 12.9352, lng: 77.6245 },
];

export const FindChargers = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  // Vehicles state
  const [vehicles, setVehicles] = useState([]);
  const [selectedVehicleId, setSelectedVehicleId] = useState('');
  const [loadingVehicles, setLoadingVehicles] = useState(true);

  // Search parameters
  const [latitude, setLatitude] = useState(12.9716);
  const [longitude, setLongitude] = useState(77.5946);
  const [radiusKm, setRadiusKm] = useState(15);
  const [maxPrice, setMaxPrice] = useState('');

  // Results state
  const [chargers, setChargers] = useState([]);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState('');

  // Booking Modal
  const [selectedChargerForBooking, setSelectedChargerForBooking] = useState(null);
  const [isBookingModalOpen, setIsBookingModalOpen] = useState(false);

  // 1. Fetch driver's vehicles
  useEffect(() => {
    const fetchVehicles = async () => {
      try {
        setLoadingVehicles(true);
        const data = await api.get('/api/driver/vehicles');
        const list = Array.isArray(data) ? data : [];
        setVehicles(list);

        const urlVehicleId = searchParams.get('vehicleId');
        if (urlVehicleId && list.some((v) => v.id.toString() === urlVehicleId)) {
          setSelectedVehicleId(urlVehicleId);
        } else if (list.length > 0) {
          setSelectedVehicleId(list[0].id.toString());
        }
      } catch (err) {
        setError('Failed to fetch your registered vehicles.');
      } finally {
        setLoadingVehicles(false);
      }
    };

    fetchVehicles();
  }, [searchParams]);

  // 2. Discover nearby chargers
  const handleSearch = async () => {
    if (!selectedVehicleId) {
      setError('Please choose a vehicle to find compatible charging stations.');
      return;
    }

    try {
      setSearching(true);
      setError('');

      let url = `/api/driver/discovery/nearby?vehicleId=${selectedVehicleId}&latitude=${latitude}&longitude=${longitude}&radiusMeters=${radiusKm * 1000}`;
      if (maxPrice) {
        url += `&maxPricePerKwh=${maxPrice}`;
      }

      const results = await api.get(url);
      setChargers(Array.isArray(results) ? results : []);
    } catch (err) {
      setError(err.message || 'Failed to search nearby chargers.');
      setChargers([]);
    } finally {
      setSearching(false);
    }
  };

  // Auto search when vehicle or coordinates are ready
  useEffect(() => {
    if (selectedVehicleId) {
      handleSearch();
    }
  }, [selectedVehicleId]);

  // Geolocation trigger
  const handleUseMyLocation = () => {
    if (!navigator.geolocation) {
      setError('Geolocation is not supported by your browser.');
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setLatitude(parseFloat(pos.coords.latitude.toFixed(6)));
        setLongitude(parseFloat(pos.coords.longitude.toFixed(6)));
      },
      () => {
        setError('Unable to retrieve your current location. Please allow location permissions.');
      }
    );
  };

  const activeVehicle = vehicles.find((v) => v.id.toString() === selectedVehicleId.toString());

  return (
    <div className="relative min-h-[calc(100vh-4.5rem)] bg-[#061224] py-10 px-4 sm:px-6 lg:px-8">
      {/* Background glow meshes */}
      <div className="absolute top-1/4 right-1/4 w-96 h-96 bg-[#00d4ff]/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 left-1/4 w-96 h-96 bg-[#635bff]/10 rounded-full blur-3xl pointer-events-none" />

      <div className="relative max-w-7xl mx-auto space-y-8">
        {/* Header Section */}
        <div className="border-b border-white/[0.08] pb-6">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-cyan-500/10 border border-cyan-500/20 text-cyan-300 text-xs font-semibold mb-2">
            <Compass className="w-3.5 h-3.5" />
            Live Geospatial PostGIS Search
          </div>
          <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
            Find Nearby EV Chargers
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Discover peer-to-peer residential & commercial chargers matching your vehicle's specific connector ports.
          </p>
        </div>

        {/* Warning if no vehicle registered */}
        {!loadingVehicles && vehicles.length === 0 && (
          <div className="p-5 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-200 text-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <Car className="w-6 h-6 text-amber-400 shrink-0" />
              <div>
                <div className="font-semibold text-white">No Electric Vehicle Registered</div>
                <div className="text-xs text-amber-300/80">
                  You need to register at least one EV to verify socket compatibility with nearby hosts.
                </div>
              </div>
            </div>
            <Link
              to="/driver/vehicles"
              className="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-xs transition-colors shrink-0"
            >
              Add EV to Garage
            </Link>
          </div>
        )}

        {/* Search Controls Card */}
        <div className="rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl p-6 shadow-xl space-y-5">
          {/* Row 1: Vehicle selector + Location inputs */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* Active Vehicle Picker */}
            <div>
              <label className="block text-xs uppercase tracking-wider text-slate-400 font-semibold mb-1.5 flex items-center justify-between">
                <span>Select Your EV:</span>
                <Link to="/driver/vehicles" className="text-cyan-400 text-[11px] hover:underline">
                  + Manage EVs
                </Link>
              </label>
              <select
                value={selectedVehicleId}
                onChange={(e) => setSelectedVehicleId(e.target.value)}
                disabled={vehicles.length === 0}
                className="w-full px-3.5 py-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-white text-sm focus:outline-none focus:border-cyan-400 disabled:opacity-50"
              >
                {vehicles.map((v) => (
                  <option key={v.id} value={v.id} className="bg-slate-900 text-white">
                    {v.manufacturer} {v.model} ({v.batteryCapacityKwh} kWh)
                  </option>
                ))}
              </select>
            </div>

            {/* Latitude */}
            <div>
              <label className="block text-xs uppercase tracking-wider text-slate-400 font-semibold mb-1.5">
                Latitude:
              </label>
              <input
                type="number"
                step="0.0001"
                value={latitude}
                onChange={(e) => setLatitude(parseFloat(e.target.value))}
                className="w-full px-3.5 py-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-white font-mono text-sm focus:outline-none focus:border-cyan-400"
              />
            </div>

            {/* Longitude + Locate Button */}
            <div>
              <div className="flex justify-between items-center mb-1.5">
                <label className="text-xs uppercase tracking-wider text-slate-400 font-semibold">
                  Longitude:
                </label>
                <button
                  type="button"
                  onClick={handleUseMyLocation}
                  className="text-[11px] text-cyan-300 hover:text-white flex items-center gap-1 font-medium"
                >
                  <Crosshair className="w-3 h-3" />
                  My Location
                </button>
              </div>
              <input
                type="number"
                step="0.0001"
                value={longitude}
                onChange={(e) => setLongitude(parseFloat(e.target.value))}
                className="w-full px-3.5 py-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-white font-mono text-sm focus:outline-none focus:border-cyan-400"
              />
            </div>
          </div>

          {/* Row 2: Location presets */}
          <div>
            <label className="block text-[11px] uppercase tracking-wider text-slate-500 font-semibold mb-2">
              📍 Quick Bengaluru Areas:
            </label>
            <div className="flex flex-wrap gap-2">
              {BANGALORE_PRESETS.map((p) => (
                <button
                  key={p.name}
                  type="button"
                  onClick={() => {
                    setLatitude(p.lat);
                    setLongitude(p.lng);
                  }}
                  className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors ${
                    latitude === p.lat && longitude === p.lng
                      ? 'bg-cyan-500/20 border-cyan-400 text-cyan-300'
                      : 'bg-white/[0.02] border-white/10 text-slate-400 hover:text-white hover:bg-white/[0.06]'
                  }`}
                >
                  {p.name}
                </button>
              ))}
            </div>
          </div>

          {/* Row 3: Radius slider & Search button */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-4 border-t border-white/[0.06]">
            <div className="w-full sm:w-auto flex items-center gap-4">
              <span className="text-xs text-slate-400 whitespace-nowrap">
                Search Radius:{' '}
                <span className="text-white font-bold font-mono">{radiusKm} km</span>
              </span>
              <input
                type="range"
                min="1"
                max="50"
                value={radiusKm}
                onChange={(e) => setRadiusKm(parseInt(e.target.value))}
                className="w-36 sm:w-48 accent-cyan-400 cursor-pointer"
              />
            </div>

            <button
              onClick={handleSearch}
              disabled={searching || !selectedVehicleId}
              className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-gradient-to-r from-[#635bff] to-[#00d4ff] text-white font-semibold text-xs shadow-lg shadow-[#635bff]/25 hover:shadow-[#635bff]/40 disabled:opacity-50 transition-all flex items-center justify-center gap-2"
            >
              {searching ? (
                <>
                  <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  Scanning PostGIS Grid...
                </>
              ) : (
                <>
                  <Search className="w-4 h-4" />
                  Search Chargers
                </>
              )}
            </button>
          </div>
        </div>

        {/* Error Notification */}
        {error && (
          <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Results Header */}
        <div className="flex items-center justify-between">
          <div className="text-sm text-slate-300">
            {searching ? (
              <span>Searching nearby stations...</span>
            ) : (
              <span>
                Found <strong className="text-cyan-300 font-bold">{chargers.length}</strong> compatible{' '}
                {chargers.length === 1 ? 'station' : 'stations'} within {radiusKm} km
              </span>
            )}
          </div>
        </div>

        {/* Chargers Grid */}
        {searching ? (
          <div className="py-20 text-center">
            <div className="inline-block w-8 h-8 border-4 border-cyan-400 border-t-transparent rounded-full animate-spin mb-4" />
            <p className="text-slate-400 text-sm">Querying PostGIS spatial indices...</p>
          </div>
        ) : chargers.length === 0 ? (
          <div className="rounded-3xl border border-white/[0.08] bg-white/[0.02] backdrop-blur-xl p-12 text-center max-w-lg mx-auto">
            <Compass className="w-12 h-12 text-slate-600 mx-auto mb-3" />
            <h3 className="text-lg font-bold text-white mb-1">No Chargers Found Nearby</h3>
            <p className="text-slate-400 text-xs mb-4">
              Try increasing your search radius to 25 or 50 km, or pick another location preset.
            </p>
            <button
              onClick={() => {
                setRadiusKm(30);
                handleSearch();
              }}
              className="px-4 py-2 rounded-xl bg-white/[0.06] hover:bg-white/[0.1] text-xs font-semibold text-cyan-300 transition-colors"
            >
              Expand Radius to 30 km
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {chargers.map((item) => {
              const c = item.charger || item;
              const distanceKm = c.distanceMeters ? (c.distanceMeters / 1000).toFixed(1) : 'Nearby';
              const isAvailable = c.availabilityStatus === 'AVAILABLE';

              return (
                <div
                  key={c.id}
                  className="rounded-3xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-xl p-6 hover:border-cyan-500/40 hover:bg-white/[0.05] transition-all flex flex-col justify-between"
                >
                  <div>
                    {/* Status & Distance Badge */}
                    <div className="flex items-center justify-between gap-2 mb-3">
                      <div className="flex items-center gap-1.5">
                        <span
                          className={`w-2 h-2 rounded-full ${
                            isAvailable ? 'bg-emerald-400 animate-pulse' : 'bg-amber-400'
                          }`}
                        />
                        <span
                          className={`text-[11px] font-semibold uppercase tracking-wider ${
                            isAvailable ? 'text-emerald-400' : 'text-amber-400'
                          }`}
                        >
                          {c.availabilityStatus || 'AVAILABLE'}
                        </span>
                      </div>
                      <span className="text-xs font-mono text-slate-400 flex items-center gap-1">
                        <MapPin className="w-3 h-3 text-cyan-400" />
                        {distanceKm} km
                      </span>
                    </div>

                    {/* Title & Host */}
                    <h3 className="text-lg font-bold text-white leading-snug mb-1">
                      {c.title}
                    </h3>
                    <p className="text-xs text-slate-400 mb-3">
                      {c.addressLine}, {c.city}
                    </p>

                    {/* Hardware specs */}
                    <div className="p-3 rounded-xl bg-white/[0.02] border border-white/[0.06] space-y-1.5 text-xs mb-4">
                      <div className="flex justify-between">
                        <span className="text-slate-400">Power Rating:</span>
                        <span className="font-semibold text-white">
                          {c.maxPowerKw} kW ({c.powerType || 'AC'})
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-400">Charging Rate:</span>
                        <span className="font-bold text-emerald-400">
                          ₹{c.pricePerKwh} / kWh
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-400">Station Host:</span>
                        <span className="text-slate-300">{c.hostName || 'Verified Host'}</span>
                      </div>
                    </div>

                    {/* Connector Ports */}
                    <div className="mb-4">
                      <span className="block text-[11px] uppercase tracking-wider text-slate-500 font-semibold mb-1.5">
                        Matched Connectors:
                      </span>
                      <div className="flex flex-wrap gap-1.5">
                        {(item.matchedConnectors || c.connectorTypes || ['CCS2']).map((conn) => (
                          <span
                            key={conn}
                            className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-cyan-500/10 text-cyan-300 border border-cyan-500/25"
                          >
                            {conn}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>

                  {/* Card Button */}
                  <div className="pt-4 border-t border-white/[0.06]">
                    <button
                      onClick={() => {
                        setSelectedChargerForBooking(c);
                        setIsBookingModalOpen(true);
                      }}
                      className="w-full py-2.5 px-4 rounded-xl bg-gradient-to-r from-[#635bff] to-[#00d4ff] text-white font-semibold text-xs shadow-lg shadow-[#635bff]/25 hover:shadow-[#635bff]/40 hover:scale-[1.01] active:scale-[0.99] transition-all flex items-center justify-center gap-1.5"
                    >
                      <Clock className="w-3.5 h-3.5" />
                      Book Charging Slot
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Book Slot Modal */}
      {selectedChargerForBooking && (
        <BookSlotModal
          charger={selectedChargerForBooking}
          vehicle={activeVehicle}
          isOpen={isBookingModalOpen}
          onClose={() => {
            setIsBookingModalOpen(false);
            setSelectedChargerForBooking(null);
          }}
          onBookingSuccess={() => {
            // refresh
          }}
        />
      )}
    </div>
  );
};
