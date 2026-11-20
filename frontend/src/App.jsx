import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext.jsx';
import { Navbar } from './components/Navbar.jsx';
import { ProtectedRoute } from './components/ProtectedRoute.jsx';
import { Home } from './pages/Home.jsx';
import { Login } from './pages/Login.jsx';
import { Register } from './pages/Register.jsx';
import { MyVehicles } from './pages/driver/MyVehicles.jsx';
import { FindChargers } from './pages/driver/FindChargers.jsx';
import { MyBookings } from './pages/driver/MyBookings.jsx';
import { ActiveSession } from './pages/driver/ActiveSession.jsx';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-emerald-500 selection:text-slate-950">
          <Navbar />
          <main className="flex-1">
            <Routes>
              {/* Public Routes */}
              <Route path="/" element={<Home />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />

              {/* Driver Routes (Protected) */}
              <Route
                path="/driver/vehicles"
                element={
                  <ProtectedRoute allowedRoles={['DRIVER']}>
                    <MyVehicles />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/driver/find"
                element={
                  <ProtectedRoute allowedRoles={['DRIVER']}>
                    <FindChargers />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/driver/bookings"
                element={
                  <ProtectedRoute allowedRoles={['DRIVER']}>
                    <MyBookings />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/driver/session"
                element={
                  <ProtectedRoute allowedRoles={['DRIVER']}>
                    <ActiveSession />
                  </ProtectedRoute>
                }
              />

              {/* Host Routes (Protected) */}
              <Route
                path="/host/dashboard"
                element={
                  <ProtectedRoute allowedRoles={['HOST']}>
                    <div className="max-w-7xl mx-auto px-4 py-12 text-center">
                      <h1 className="text-3xl font-extrabold text-white mb-2">Host Dashboard</h1>
                      <p className="text-slate-400">Manage your private EV chargers and schedule availability.</p>
                    </div>
                  </ProtectedRoute>
                }
              />
              <Route
                path="/host/earnings"
                element={
                  <ProtectedRoute allowedRoles={['HOST']}>
                    <div className="max-w-7xl mx-auto px-4 py-12 text-center">
                      <h1 className="text-3xl font-extrabold text-white mb-2">Earnings & Payouts</h1>
                      <p className="text-slate-400">Dynamic ledger accounting and instant UPI withdrawals.</p>
                    </div>
                  </ProtectedRoute>
                }
              />

              {/* Admin Routes (Protected) */}
              <Route
                path="/admin"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <div className="max-w-7xl mx-auto px-4 py-12 text-center">
                      <h1 className="text-3xl font-extrabold text-white mb-2">Admin CEO Dashboard</h1>
                      <p className="text-slate-400">Review charger hardware, verify stations, and approve payouts.</p>
                    </div>
                  </ProtectedRoute>
                }
              />

              {/* Fallback */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>

          {/* Footer (Stripe Style) */}
          <footer className="border-t border-slate-900 bg-slate-950 py-8 text-center text-xs text-slate-500">
            <div className="max-w-7xl mx-auto px-4">
              <p>© 2026 ChargeConnect Inc. Decentralized P2P EV Charging Network.</p>
              <p className="mt-1 text-slate-600">Built with Spring Boot 4, PostGIS 3.7, Redis 7 & React</p>
            </div>
          </footer>
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
}
