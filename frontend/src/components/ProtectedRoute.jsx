import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export const ProtectedRoute = ({ allowedRoles, children }) => {
  const { user, token, loading, hasRole } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="min-h-[70vh] flex flex-col items-center justify-center gap-3">
        <div className="w-10 h-10 border-4 border-emerald-500/20 border-t-emerald-400 rounded-full animate-spin"></div>
        <p className="text-sm font-medium text-slate-400">Verifying session...</p>
      </div>
    );
  }

  if (!token || !user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && allowedRoles.length > 0) {
    const hasRequiredRole = allowedRoles.some((role) => hasRole(role));
    if (!hasRequiredRole) {
      return (
        <div className="max-w-md mx-auto mt-20 p-8 bg-[#091a33]/90 backdrop-blur-2xl border border-rose-500/30 rounded-3xl text-center shadow-2xl">
          <div className="w-14 h-14 bg-rose-500/10 text-rose-400 rounded-2xl flex items-center justify-center mx-auto mb-4 text-2xl">
            🔒
          </div>
          <h2 className="text-xl font-bold text-white mb-2">Access Restricted</h2>
          <p className="text-slate-400 text-sm mb-6 leading-relaxed">
            Your account ({user.roles.map((r) => r.replace('ROLE_', '')).join(', ')}) does not have permission to view
            this dashboard.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
            {hasRole('HOST') && (
              <a
                href="/host/dashboard"
                className="px-5 py-2.5 bg-gradient-to-r from-[#635bff] to-[#a855f7] text-white text-xs font-semibold rounded-xl transition-all shadow-md"
              >
                Go to Host Hub →
              </a>
            )}
            {hasRole('ADMIN') && (
              <a
                href="/admin"
                className="px-5 py-2.5 bg-gradient-to-r from-amber-500 to-orange-500 text-slate-950 text-xs font-bold rounded-xl transition-all shadow-md"
              >
                Go to Admin CEO →
              </a>
            )}
            <a
              href="/"
              className="px-5 py-2.5 bg-white/[0.06] hover:bg-white/[0.1] text-white text-xs font-semibold rounded-xl transition-all"
            >
              Back to Home
            </a>
          </div>
        </div>
      );
    }
  }

  return children;
};
