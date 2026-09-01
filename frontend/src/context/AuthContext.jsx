import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi } from '../api/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchCurrentUser = useCallback(async () => {
    try {
      const res = await authApi.getCurrentUser();
      if (res && res.data) {
        setUser(res.data);
      } else {
        setUser(null);
      }
    } catch (err) {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCurrentUser();
  }, [fetchCurrentUser]);

  const login = async (username, password, rememberMe = false) => {
    const res = await authApi.login(username, password, rememberMe);
    setUser(res.data);
    return res.data;
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch (err) {
      console.error('Logout error:', err);
    } finally {
      setUser(null);
    }
  };

  const roleClean = (role) => (role || '').replace('ROLE_', '').toUpperCase();
  const isAdmin = Boolean(user && roleClean(user.role) === 'ADMIN');
  const isOperator = Boolean(user && (roleClean(user.role) === 'OPERATOR' || isAdmin));
  const isViewer = Boolean(user && roleClean(user.role) === 'VIEWER');

  const value = {
    user,
    loading,
    isAuthenticated: Boolean(user),
    isAdmin,
    isOperator,
    isViewer,
    login,
    logout,
    refreshUser: fetchCurrentUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
