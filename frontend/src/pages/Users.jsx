import React, { useState, useEffect, useCallback } from 'react';
import { useToast } from '../context/ToastContext';
import { usersApi } from '../api/users';
import {
  Users as UsersIcon,
  UserPlus,
  Lock,
  Unlock,
  ShieldCheck,
  ShieldAlert,
  Wrench,
  Eye,
  RefreshCw,
  Clock,
  User
} from 'lucide-react';

export default function Users() {
  const toast = useToast();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  // New User Form State
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [role, setRole] = useState('ROLE_OPERATOR');
  const [shift, setShift] = useState('Shift 1 (07:00 - 15:30)');
  const [createLoading, setCreateLoading] = useState(false);

  const fetchUsers = useCallback(async () => {
    try {
      setLoading(true);
      const data = await usersApi.getAll();
      setUsers(data);
    } catch (err) {
      toast.error('Failed to load users list');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleCreateUser = async (e) => {
    e.preventDefault();
    if (!username.trim() || !password) {
      toast.warning('Username and password are required!');
      return;
    }

    setCreateLoading(true);
    try {
      await usersApi.create({
        username: username.trim(),
        password,
        fullName: fullName.trim() || username.trim(),
        role,
        shift,
      });
      toast.success(`User "${username}" created successfully!`);
      setUsername('');
      setPassword('');
      setFullName('');
      fetchUsers();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error creating user');
    } finally {
      setCreateLoading(false);
    }
  };

  const handleToggleStatus = async (userObj) => {
    try {
      const updated = await usersApi.toggleStatus(userObj.id);
      toast.success(
        updated.active
          ? `Access granted for ${userObj.username}`
          : `Access blocked for ${userObj.username}`
      );
      fetchUsers();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error updating user status');
    }
  };

  const getRoleBadge = (r) => {
    const clean = (r || '').replace('ROLE_', '').toUpperCase();
    if (clean === 'ADMIN') {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20">
          <ShieldAlert className="w-3 h-3" /> Administrator
        </span>
      );
    }
    if (clean === 'OPERATOR') {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-brand-500/10 text-brand-400 border border-brand-500/20">
          <Wrench className="w-3 h-3" /> Operator
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
        <Eye className="w-3 h-3" /> Viewer (Read-Only)
      </span>
    );
  };

  return (
    <div className="space-y-8 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-16 animate-fade-in">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 glass-panel p-6 rounded-3xl border border-slate-800">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight flex items-center gap-2.5">
            <UsersIcon className="w-8 h-8 text-brand-400" />
            User Management & Access Control
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Manage WMS access accounts, assign role permissions, and secure user states
          </p>
        </div>

        <button
          onClick={fetchUsers}
          className="p-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors self-start sm:self-auto"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-brand-400' : ''}`} />
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Create User Form (Col 4) */}
        <div className="lg:col-span-4 glass-card p-6 rounded-3xl border border-slate-800 h-fit">
          <div className="flex items-center gap-2.5 pb-4 mb-4 border-b border-slate-800">
            <div className="p-2 rounded-xl bg-brand-500/10 text-brand-400 border border-brand-500/20">
              <UserPlus className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">New User</h3>
              <p className="text-xs text-slate-400">Create account and assign permissions</p>
            </div>
          </div>

          <form onSubmit={handleCreateUser} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                Username *
              </label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="e.g. john.doe"
                required
                className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm text-white placeholder-slate-500 outline-none focus:border-brand-500 transition-colors"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                Full Name
              </label>
              <input
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="e.g. John Doe"
                className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm text-white placeholder-slate-500 outline-none focus:border-brand-500 transition-colors"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                Initial Password *
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                required
                className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm text-white placeholder-slate-500 outline-none focus:border-brand-500 transition-colors"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                System Role *
              </label>
              <select
                value={role}
                onChange={(e) => setRole(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm text-white outline-none focus:border-brand-500 transition-colors"
              >
                <option value="ROLE_ADMIN">Administrator (Full Access)</option>
                <option value="ROLE_OPERATOR">Warehouse Operator (Picking/Inventory)</option>
                <option value="ROLE_VIEWER">Viewer (Read-Only / Audit)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                Assigned Shift
              </label>
              <select
                value={shift}
                onChange={(e) => setShift(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm text-white outline-none focus:border-brand-500 transition-colors"
              >
                <option value="Shift 1 (07:00 - 15:30)">Shift 1 (07:00 - 15:30)</option>
                <option value="Shift 2 (15:30 - 00:00)">Shift 2 (15:30 - 00:00)</option>
                <option value="Night Shift (00:00 - 07:00)">Night Shift (00:00 - 07:00)</option>
              </select>
            </div>

            <button
              type="submit"
              disabled={createLoading}
              className="w-full py-3 bg-brand-600 hover:bg-brand-500 text-white font-bold rounded-xl shadow-lg shadow-brand-600/30 transition-all flex items-center justify-center gap-2 text-sm disabled:opacity-50 mt-2"
            >
              <UserPlus className="w-4 h-4" />
              <span>{createLoading ? 'Saving...' : 'SAVE USER'}</span>
            </button>
          </form>
        </div>

        {/* Users Table (Col 8) */}
        <div className="lg:col-span-8 glass-card rounded-3xl border border-slate-800 overflow-hidden shadow-2xl">
          <div className="p-6 border-b border-slate-800/80 bg-slate-900/40">
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <UsersIcon className="w-5 h-5 text-brand-400" />
              Registered Staff &bull; Active Accounts
            </h3>
            <p className="text-xs text-slate-400 mt-0.5">Control access states and system privileges</p>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-900/80 text-[11px] font-bold uppercase tracking-wider text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="px-6 py-4">User & Name</th>
                  <th className="px-6 py-4">Assigned Role</th>
                  <th className="px-6 py-4">Shift / Access</th>
                  <th className="px-6 py-4 text-center">Status</th>
                  <th className="px-6 py-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-medium">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-800/40 transition-colors">
                    <td className="px-6 py-4">
                      <div className="font-semibold text-white">{u.fullName || u.username}</div>
                      <div className="text-xs font-mono text-slate-400">@{u.username}</div>
                    </td>

                    <td className="px-6 py-4">{getRoleBadge(u.role)}</td>

                    <td className="px-6 py-4 text-xs text-slate-300">
                      <div>{u.shift || 'General'}</div>
                      {u.lastLogin && (
                        <div className="text-[10px] text-slate-500 font-mono mt-0.5">
                          Last login: {new Date(u.lastLogin).toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                        </div>
                      )}
                    </td>

                    <td className="px-6 py-4 text-center">
                      {u.active ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                          <ShieldCheck className="w-3 h-3" /> ACTIVE
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                          <ShieldAlert className="w-3 h-3" /> BLOCKED
                        </span>
                      )}
                    </td>

                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => handleToggleStatus(u)}
                        className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold border transition-all ${
                          u.active
                            ? 'bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border-rose-500/20'
                            : 'bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border-emerald-500/20'
                        }`}
                      >
                        {u.active ? <Lock className="w-3.5 h-3.5" /> : <Unlock className="w-3.5 h-3.5" />}
                        <span>{u.active ? 'Block' : 'Unblock'}</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
