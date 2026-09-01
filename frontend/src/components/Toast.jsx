import React, { useEffect } from 'react';
import { CheckCircle2, AlertTriangle, XCircle, Info, X } from 'lucide-react';

function ToastItem({ toast, onRemove }) {
  useEffect(() => {
    const timer = setTimeout(() => {
      onRemove(toast.id);
    }, toast.duration || 4000);
    return () => clearTimeout(timer);
  }, [toast, onRemove]);

  const icons = {
    success: <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />,
    error: <XCircle className="w-5 h-5 text-rose-400 shrink-0" />,
    warning: <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0" />,
    info: <Info className="w-5 h-5 text-brand-400 shrink-0" />,
  };

  const borders = {
    success: 'border-emerald-500/30 bg-emerald-950/80 text-emerald-100 shadow-emerald-950/50',
    error: 'border-rose-500/30 bg-rose-950/80 text-rose-100 shadow-rose-950/50',
    warning: 'border-amber-500/30 bg-amber-950/80 text-amber-100 shadow-amber-950/50',
    info: 'border-brand-500/30 bg-slate-900/90 text-brand-100 shadow-brand-950/50',
  };

  return (
    <div
      className={`flex items-start gap-3 p-4 rounded-xl border backdrop-blur-xl shadow-xl transition-all duration-300 animate-scale-in max-w-md w-full pointer-events-auto ${borders[toast.type] || borders.info}`}
    >
      {icons[toast.type] || icons.info}
      <div className="flex-1 text-sm font-medium leading-relaxed">{toast.message}</div>
      <button
        onClick={() => onRemove(toast.id)}
        className="text-slate-400 hover:text-white transition-colors p-0.5 rounded-lg hover:bg-white/10"
      >
        <X className="w-4 h-4" />
      </button>
    </div>
  );
}

export default function ToastContainer({ toasts, onRemove }) {
  return (
    <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2 pointer-events-none">
      {toasts.map((t) => (
        <ToastItem key={t.id} toast={t} onRemove={onRemove} />
      ))}
    </div>
  );
}
