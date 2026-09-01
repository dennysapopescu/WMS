import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import { CurrencyProvider } from './context/CurrencyContext';
import ProtectedRoute from './components/ProtectedRoute';
import Navbar from './components/Navbar';

import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Locations from './pages/Locations';
import WarehouseMap from './pages/WarehouseMap';
import Picking from './pages/Picking';
import Users from './pages/Users';
import Profile from './pages/Profile';

function Layout({ children }) {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col selection:bg-brand-500 selection:text-white">
      <Navbar />
      <main className="flex-1">{children}</main>
    </div>
  );
}

export default function App() {
  return (
    <ToastProvider>
      <AuthProvider>
        <CurrencyProvider>
          <Routes>

          {/* Public Auth Route */}
          <Route path="/login" element={<Login />} />

          {/* Protected Application Routes */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Layout>
                  <Dashboard />
                </Layout>
              </ProtectedRoute>
            }
          />

          <Route
            path="/locations"
            element={
              <ProtectedRoute>
                <Layout>
                  <Locations />
                </Layout>
              </ProtectedRoute>
            }
          />

          <Route
            path="/map"
            element={
              <ProtectedRoute>
                <Layout>
                  <WarehouseMap />
                </Layout>
              </ProtectedRoute>
            }
          />

          <Route
            path="/picking"
            element={
              <ProtectedRoute>
                <Layout>
                  <Picking />
                </Layout>
              </ProtectedRoute>
            }
          />

          {/* Route alias for orders */}
          <Route
            path="/orders"
            element={<Navigate to="/picking" replace />}
          />

          <Route
            path="/users"
            element={
              <ProtectedRoute requiredRole="ADMIN">
                <Layout>
                  <Users />
                </Layout>
              </ProtectedRoute>
            }
          />

          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <Layout>
                  <Profile />
                </Layout>
              </ProtectedRoute>
            }
          />

          {/* Catch-all fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        </CurrencyProvider>
      </AuthProvider>
    </ToastProvider>

  );
}
