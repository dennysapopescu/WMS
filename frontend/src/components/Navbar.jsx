import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCurrency } from '../context/CurrencyContext';
import {
  Boxes,
  LayoutDashboard,
  Grid3X3,
  Map,
  ListOrdered,
  Users,
  UserCircle,
  LogOut,
  Menu,
  X,
  ShieldAlert,
  Eye,
  Wrench,
  DollarSign
} from 'lucide-react';


export default function Navbar() {
  const { user, isAdmin, isViewer, logout } = useAuth();
  const { currencyCode, setCurrency, supportedCurrencies } = useCurrency();
  const location = useLocation();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const navLinks = [
    { to: '/', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/locations', label: 'Locations', icon: Grid3X3 },
    { to: '/map', label: 'Warehouse Map', icon: Map, highlight: true },
    { to: '/picking', label: 'Picking List', icon: ListOrdered },
    ...(isAdmin ? [{ to: '/users', label: 'Users', icon: Users }] : []),
    { to: '/profile', label: 'Profile', icon: UserCircle },
  ];

  const getRoleBadge = (role) => {
    const clean = (role || '').replace('ROLE_', '').toUpperCase();
    if (clean === 'ADMIN') {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20">
          <ShieldAlert className="w-3 h-3" /> ADMIN
        </span>
      );
    }
    if (clean === 'OPERATOR') {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-brand-500/10 text-brand-400 border border-brand-500/20">
          <Wrench className="w-3 h-3" /> OPERATOR
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
        <Eye className="w-3 h-3" /> VIEWER
      </span>
    );
  };

  return (
    <nav className="sticky top-0 z-40 w-full glass-panel border-b border-slate-800/80 mb-6">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Brand Logo */}
          <div className="flex items-center gap-3">
            <Link to="/" className="flex items-center gap-2.5 group">
              <div className="p-2 rounded-xl bg-gradient-to-tr from-brand-600 to-indigo-500 text-white shadow-lg shadow-brand-600/30 group-hover:scale-105 transition-transform duration-200">
                <Boxes className="w-6 h-6" />
              </div>
              <div className="flex flex-col">
                <span className="text-lg font-bold tracking-tight text-white flex items-center gap-1.5">
                  WMS
                </span>
                <span className="text-[10px] uppercase font-bold tracking-widest text-slate-400">Warehouse Management System</span>
              </div>
            </Link>
          </div>

          {/* Desktop Nav Links */}
          <div className="hidden md:flex items-center gap-1.5">
            {navLinks.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname === item.to;

              if (item.highlight) {
                return (
                  <Link
                    key={item.to}
                    to={item.to}
                    className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-sm font-semibold transition-all duration-200 ${
                      isActive
                        ? 'bg-brand-600 text-white shadow-lg shadow-brand-600/40 glow-blue'
                        : 'bg-brand-500/10 text-brand-300 hover:bg-brand-500/20 border border-brand-500/30'
                    }`}
                  >
                    <Icon className="w-4 h-4" />
                    <span>{item.label}</span>
                  </Link>
                );
              }

              return (
                <Link
                  key={item.to}
                  to={item.to}
                  className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-sm font-medium transition-all duration-200 ${
                    isActive
                      ? 'bg-slate-800 text-white font-semibold shadow-inner border border-slate-700'
                      : 'text-slate-300 hover:text-white hover:bg-slate-800/60'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span>{item.label}</span>
                </Link>
              );
            })}
          </div>

          {/* User Profile, Currency Selector & Logout */}
          <div className="hidden md:flex items-center gap-3">
            {/* Currency Selector */}
            <div className="flex items-center gap-1 px-2.5 py-1.5 rounded-xl bg-slate-900/90 border border-slate-800 text-xs font-semibold text-slate-300 shadow-sm" title="Select Display Currency">
              <DollarSign className="w-3.5 h-3.5 text-amber-400" />
              <select
                value={currencyCode}
                onChange={(e) => setCurrency(e.target.value)}
                className="bg-transparent text-white font-mono text-xs outline-none cursor-pointer pr-1"
                aria-label="Currency"
              >
                {supportedCurrencies.map((c) => (
                  <option key={c.code} value={c.code} className="bg-slate-900 text-white">
                    {c.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex flex-col items-end">
              <div className="flex items-center gap-2">
                <span className="text-sm font-semibold text-white tracking-wide">
                  {user?.fullName || user?.username || 'User'}
                </span>
                {getRoleBadge(user?.role)}
              </div>
              <span className="text-xs text-slate-400 font-mono">@{user?.username}</span>
            </div>

            <button
              onClick={handleLogout}
              title="Log out"
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold text-rose-300 bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/20 transition-all hover:scale-105"
            >
              <LogOut className="w-4 h-4" />
              <span>Log out</span>
            </button>
          </div>

          {/* Mobile Menu Button */}
          <div className="flex md:hidden">
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Menu Dropdown */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-slate-800 bg-slate-950/95 backdrop-blur-xl px-4 pt-3 pb-5 space-y-2 animate-fade-in">
          {/* User profile header in mobile */}
          <div className="p-3 bg-slate-900 rounded-xl mb-3 flex items-center justify-between border border-slate-800">
            <div>
              <div className="text-sm font-bold text-white">{user?.fullName || user?.username}</div>
              <div className="text-xs text-slate-400">@{user?.username}</div>
            </div>
            {getRoleBadge(user?.role)}
          </div>

          {/* Currency Selector in Mobile */}
          <div className="p-3 bg-slate-900/80 rounded-xl flex items-center justify-between border border-slate-800 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
              <DollarSign className="w-3.5 h-3.5 text-amber-400" /> Currency
            </span>
            <select
              value={currencyCode}
              onChange={(e) => setCurrency(e.target.value)}
              className="bg-slate-950 px-2.5 py-1 rounded-lg border border-slate-700 text-white font-mono text-xs outline-none"
            >
              {supportedCurrencies.map((c) => (
                <option key={c.code} value={c.code}>
                  {c.label}
                </option>
              ))}
            </select>
          </div>


          {navLinks.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.to;
            return (
              <Link
                key={item.to}
                to={item.to}
                onClick={() => setMobileMenuOpen(false)}
                className={`flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-brand-600 text-white font-semibold'
                    : 'text-slate-300 hover:bg-slate-800/80 hover:text-white'
                }`}
              >
                <Icon className="w-5 h-5" />
                <span>{item.label}</span>
              </Link>
            );
          })}

          <button
            onClick={() => {
              setMobileMenuOpen(false);
              handleLogout();
            }}
            className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-semibold text-rose-400 bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/20 mt-2 transition-colors"
          >
            <LogOut className="w-5 h-5" />
            <span>Log out</span>
          </button>
        </div>
      )}
    </nav>
  );
}
