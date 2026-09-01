import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import {
  Boxes,
  Lock,
  User,
  ArrowRight,
  ShieldCheck,
  Zap,
  Sparkles,
  Eye,
  Wrench,
  ShieldAlert
} from 'lucide-react';

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const { login } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation();

  const from = location.state?.from?.pathname || '/';

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!username.trim() || !password) {
      setError('Please enter both username and password.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      await login(username.trim(), password, rememberMe);
      toast.success(`Welcome back, ${username}!`);
      navigate(from, { replace: true });
    } catch (err) {
      console.error(err);
      const msg = err.response?.data?.message || 'Invalid username or password!';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleQuickLogin = (u, p) => {
    setUsername(u);
    setPassword(p);
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 relative overflow-hidden bg-slate-950">
      {/* Dynamic Background Glows */}
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-brand-600/20 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-indigo-600/20 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-sky-500/5 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-md relative z-10 animate-scale-in">
        {/* Logo and Brand Header */}
        <div className="text-center mb-8">
          <div className="inline-flex p-3 rounded-2xl bg-gradient-to-tr from-brand-600 to-indigo-500 text-white shadow-xl shadow-brand-600/30 mb-4 animate-bounce">
            <Boxes className="w-10 h-10" />
          </div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">
            WMS
          </h1>
          <p className="text-sm text-slate-400 mt-1">Modern Warehouse Management & Logistics System</p>
        </div>

        {/* Login Card */}
        <div className="glass-card p-8 rounded-3xl border border-slate-800 shadow-2xl shadow-black/80">
          <form onSubmit={handleSubmit} className="space-y-5">
            {error && (
              <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm font-medium animate-fade-in flex items-center gap-2.5">
                <ShieldAlert className="w-5 h-5 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Username
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <User className="w-5 h-5" />
                </div>
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="e.g. admin / operator / viewer"
                  required
                  className="w-full pl-11 pr-4 py-3 bg-slate-900/80 border border-slate-800 focus:border-brand-500 focus:ring-2 focus:ring-brand-500/20 rounded-xl text-sm font-medium text-white placeholder-slate-500 transition-all outline-none"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Password
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <Lock className="w-5 h-5" />
                </div>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  className="w-full pl-11 pr-4 py-3 bg-slate-900/80 border border-slate-800 focus:border-brand-500 focus:ring-2 focus:ring-brand-500/20 rounded-xl text-sm font-medium text-white placeholder-slate-500 transition-all outline-none"
                />
              </div>
            </div>

            <div className="flex items-center justify-between">
              <label className="flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  className="w-4 h-4 rounded border-slate-800 text-brand-600 focus:ring-brand-500 bg-slate-900"
                />
                <span className="text-xs text-slate-400 font-medium">Remember me</span>
              </label>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 px-4 bg-gradient-to-r from-brand-600 to-indigo-600 hover:from-brand-500 hover:to-indigo-500 text-white font-bold rounded-xl shadow-lg shadow-brand-600/30 transition-all duration-200 flex items-center justify-center gap-2 text-sm disabled:opacity-50 disabled:cursor-not-allowed group hover:scale-[1.01]"
            >
              {loading ? (
                <span>Signing in...</span>
              ) : (
                <>
                  <span>SIGN IN</span>
                  <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
                </>
              )}
            </button>
          </form>

          {/* Quick Demo Logins Helper */}
          <div className="mt-8 pt-6 border-t border-slate-800/80">
            <div className="flex items-center gap-1.5 justify-center mb-3">
              <Sparkles className="w-3.5 h-3.5 text-amber-400" />
              <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                Quick Demo Logins
              </span>
            </div>

            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => handleQuickLogin('admin', 'admin123')}
                className="p-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 hover:border-rose-500/40 text-center transition-all group"
              >
                <ShieldAlert className="w-4 h-4 text-rose-400 mx-auto mb-1 group-hover:scale-110 transition-transform" />
                <span className="text-xs font-bold text-slate-200 block">Admin</span>
              </button>

              <button
                type="button"
                onClick={() => handleQuickLogin('operator', 'operator123')}
                className="p-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 hover:border-brand-500/40 text-center transition-all group"
              >
                <Wrench className="w-4 h-4 text-brand-400 mx-auto mb-1 group-hover:scale-110 transition-transform" />
                <span className="text-xs font-bold text-slate-200 block">Operator</span>
              </button>

              <button
                type="button"
                onClick={() => handleQuickLogin('viewer', 'viewer123')}
                className="p-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 hover:border-emerald-500/40 text-center transition-all group"
              >
                <Eye className="w-4 h-4 text-emerald-400 mx-auto mb-1 group-hover:scale-110 transition-transform" />
                <span className="text-xs font-bold text-slate-200 block">Viewer</span>
              </button>
            </div>
          </div>
        </div>

        <div className="text-center mt-6 text-xs text-slate-400">
          &copy; {new Date().getFullYear()} WMS
        </div>
      </div>
    </div>
  );
}
