import React, { createContext, useContext, useState, useEffect } from 'react';
import { api } from '../api/client.js';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('chargeconnect_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState(() => {
    return localStorage.getItem('chargeconnect_token');
  });
  const [loading, setLoading] = useState(true);

  const fetchProfile = async () => {
    const profile = await api.get('/api/users/me');
    setUser(profile);
    localStorage.setItem('chargeconnect_user', JSON.stringify(profile));
    return profile;
  };

  useEffect(() => {
    if (token) {
      fetchProfile()
        .catch(() => {
          logout();
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, [token]);

  const login = async (email, password) => {
    const res = await api.post('/api/auth/login', { email, password });
    localStorage.setItem('chargeconnect_token', res.token);
    setToken(res.token);
    const profile = await fetchProfile();
    return profile;
  };

  const register = async (fullName, email, phone, password, role) => {
    const res = await api.post('/api/auth/register', {
      fullName,
      email,
      phone: phone?.trim() ? phone.trim() : null,
      password,
      role,
    });
    localStorage.setItem('chargeconnect_token', res.token);
    setToken(res.token);
    const profile = await fetchProfile();
    return profile;
  };

  const logout = () => {
    localStorage.removeItem('chargeconnect_token');
    localStorage.removeItem('chargeconnect_user');
    setToken(null);
    setUser(null);
  };

  const hasRole = (role) => {
    if (!user || !user.roles) return false;
    const cleanRole = role.replace('ROLE_', '');
    return user.roles.some((r) => r.replace('ROLE_', '') === cleanRole);
  };

  const refreshProfile = async () => {
    if (token) {
      await fetchProfile();
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        loading,
        login,
        register,
        logout,
        hasRole,
        refreshProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
