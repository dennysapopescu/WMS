import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { profileApi } from '../api/profile';
import {
  UserCircle,
  KeyRound,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  Clock,
  User,
  Shield
} from 'lucide-react';

export default function Profile() {
  const { user } = useAuth();
  const toast = useToast();

  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const passwordsMatch = newPassword && confirmPassword && newPassword === confirmPassword;
  const passwordsMismatch = newPassword && confirmPassword && newPassword !== confirmPassword;

  const handleChangePassword = async (e) => {
    e.preventDefault();
    if (!oldPassword || !newPassword || !confirmPassword) {
      toast.warning('Please fill in all password fields!');
      return;
    }

    if (newPassword.length < 4) {
      toast.warning('New password must be at least 4 characters!');
      return;
    }

    if (newPassword !== confirmPassword) {
      toast.error('New password and confirmation do not match!');
      return;
    }

    setLoading(true);
    try {
      await profileApi.changePassword(oldPassword, newPassword, confirmPassword);
      toast.success('Password updated successfully!');
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Current password is incorrect!');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 pb-16 space-y-8 animate-fade-in">
      {/* Header */}
      <div className="glass-panel p-6 rounded-3xl border border-slate-800 flex items-center justify-between">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight flex items-center gap-2.5">
            <UserCircle className="w-8 h-8 text-brand-400" />
            User Profile & Security
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Account information and access credentials management
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-12 gap-8">
        {/* User Card (Col 5) */}
        <div className="md:col-span-5 glass-card p-6 rounded-3xl border border-slate-800 flex flex-col items-center text-center">
          <div className="w-24 h-24 rounded-full bg-gradient-to-tr from-brand-600 to-indigo-600 flex items-center justify-center text-white shadow-xl shadow-brand-600/30 mb-4">
            <UserCircle className="w-16 h-16" />
          </div>

          <h2 className="text-xl font-bold text-white">{user?.fullName || user?.username}</h2>
          <span className="text-xs font-mono text-slate-400 mt-0.5">@{user?.username}</span>

          <div className="mt-4 px-3 py-1 rounded-full text-xs font-bold bg-brand-500/10 text-brand-400 border border-brand-500/20">
            {user?.role || 'ROLE_OPERATOR'}
          </div>

          <div className="w-full mt-6 pt-6 border-t border-slate-800 text-left space-y-3 text-xs">
            <div className="flex items-center justify-between">
              <span className="text-slate-400">Shift:</span>
              <span className="font-semibold text-slate-200">{user?.shift || 'Shift 1'}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-400">Account Status:</span>
              <span className="font-bold text-emerald-400 flex items-center gap-1">
                <ShieldCheck className="w-3.5 h-3.5" /> Active
              </span>
            </div>
          </div>
        </div>

        {/* Change Password Form (Col 7) */}
        <div className="md:col-span-7 glass-card p-6 rounded-3xl border border-slate-800">
          <div className="flex items-center gap-2.5 pb-4 mb-4 border-b border-slate-800">
            <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
              <KeyRound className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">Change Password</h3>
              <p className="text-xs text-slate-400">Update your account password for enhanced security</p>
            </div>
          </div>

          <form onSubmit={handleChangePassword} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                Current Password *
              </label>
              <input
                type="password"
                value={oldPassword}
                onChange={(e) => setOldPassword(e.target.value)}
                placeholder="••••••••"
                required
                className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm text-white placeholder-slate-500 outline-none focus:border-brand-500 transition-colors"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                New Password (Min. 4 characters) *
              </label>
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="••••••••"
                required
                minLength={4}
                className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm text-white placeholder-slate-500 outline-none focus:border-brand-500 transition-colors"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                Confirm New Password *
              </label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="••••••••"
                required
                className={`w-full px-3.5 py-2.5 bg-slate-900 border rounded-xl text-sm text-white placeholder-slate-500 outline-none transition-colors ${
                  passwordsMatch
                    ? 'border-emerald-500 focus:border-emerald-500'
                    : passwordsMismatch
                    ? 'border-rose-500 focus:border-rose-500'
                    : 'border-slate-800 focus:border-brand-500'
                }`}
              />

              {/* Match indicator */}
              {passwordsMatch && (
                <div className="mt-1.5 flex items-center gap-1.5 text-xs text-emerald-400 font-medium">
                  <CheckCircle2 className="w-3.5 h-3.5" /> Passwords match!
                </div>
              )}
              {passwordsMismatch && (
                <div className="mt-1.5 flex items-center gap-1.5 text-xs text-rose-400 font-medium">
                  <AlertCircle className="w-3.5 h-3.5" /> Passwords do not match!
                </div>
              )}
            </div>

            <button
              type="submit"
              disabled={loading || Boolean(passwordsMismatch)}
              className="w-full py-3 bg-indigo-600 hover:bg-indigo-500 text-white font-bold rounded-xl shadow-lg shadow-indigo-600/30 transition-all flex items-center justify-center gap-2 text-sm disabled:opacity-50 mt-4"
            >
              <KeyRound className="w-4 h-4" />
              <span>{loading ? 'Updating...' : 'UPDATE PASSWORD'}</span>
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
