import React from 'react';

export default function StatCard({ title, value, subtitle, unit, icon: Icon, color = 'blue', onClick }) {
  const colorSchemes = {
    blue: {
      border: 'border-blue-500/20 hover:border-blue-500/40',
      iconBg: 'bg-blue-500/10 text-blue-400 border border-blue-500/20',
      glow: 'glow-blue',
      badge: 'text-blue-400 bg-blue-500/10',
      accent: 'from-blue-600 to-indigo-600',
    },
    emerald: {
      border: 'border-emerald-500/20 hover:border-emerald-500/40',
      iconBg: 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20',
      glow: 'glow-emerald',
      badge: 'text-emerald-400 bg-emerald-500/10',
      accent: 'from-emerald-600 to-teal-600',
    },
    rose: {
      border: 'border-rose-500/20 hover:border-rose-500/40',
      iconBg: 'bg-rose-500/10 text-rose-400 border border-rose-500/20',
      glow: 'glow-rose',
      badge: 'text-rose-400 bg-rose-500/10',
      accent: 'from-rose-600 to-pink-600',
    },
    amber: {
      border: 'border-amber-500/20 hover:border-amber-500/40',
      iconBg: 'bg-amber-500/10 text-amber-400 border border-amber-500/20',
      glow: 'glow-amber',
      badge: 'text-amber-400 bg-amber-500/10',
      accent: 'from-amber-600 to-orange-600',
    },
    purple: {
      border: 'border-purple-500/20 hover:border-purple-500/40',
      iconBg: 'bg-purple-500/10 text-purple-400 border border-purple-500/20',
      glow: 'glow-blue',
      badge: 'text-purple-400 bg-purple-500/10',
      accent: 'from-purple-600 to-indigo-600',
    },
  };

  const scheme = colorSchemes[color] || colorSchemes.blue;

  return (
    <div
      onClick={onClick}
      className={`glass-card p-5 rounded-2xl border transition-all duration-300 relative overflow-hidden group ${scheme.border} ${scheme.glow} ${onClick ? 'cursor-pointer hover:-translate-y-1' : ''}`}
    >
      {/* Decorative top accent line */}
      <div className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${scheme.accent} opacity-80`} />

      <div className="flex items-center justify-between gap-4">
        <div>
          <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">{title}</span>
          <div className="flex items-baseline gap-1.5 mt-1.5">
            <h3 className="text-2xl lg:text-3xl font-extrabold text-white tracking-tight">{value}</h3>
            {unit && <span className="text-sm font-medium text-slate-400">{unit}</span>}
          </div>
          {subtitle && (
            <div className="mt-2 flex items-center gap-1.5">
              <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${scheme.badge}`}>{subtitle}</span>
            </div>
          )}
        </div>

        {Icon && (
          <div className={`p-3.5 rounded-2xl shrink-0 transition-transform duration-300 group-hover:scale-110 ${scheme.iconBg}`}>
            <Icon className="w-6 h-6" />
          </div>
        )}
      </div>
    </div>
  );
}
