import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { ordersApi } from '../api/orders';
import QrScannerModal from '../components/QrScannerModal';
import {
  ListOrdered,
  QrCode,
  CheckCircle2,
  Trash2,
  Clock,
  MapPin,
  PackageCheck,
  RefreshCw,
  Eye,
  AlertCircle
} from 'lucide-react';

export default function Picking() {
  const { isViewer } = useAuth();
  const toast = useToast();

  const [orders, setOrders] = useState([]);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);

  // Scanner modal state
  const [activeScanOrder, setActiveScanOrder] = useState(null);

  const fetchOrdersData = useCallback(async () => {
    try {
      setLoading(true);
      const [allOrders, hist] = await Promise.all([
        ordersApi.getAll(),
        ordersApi.getHistory(),
      ]);
      setOrders(allOrders);
      setHistory(hist);
    } catch (err) {
      toast.error('Failed to load picking list');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    fetchOrdersData();
  }, [fetchOrdersData]);

  // Trigger QR Scanner for Order
  const handleOpenScanner = (order) => {
    setActiveScanOrder(order);
  };

  // On QR Scan Confirmation
  const handleScanSuccess = async (scannedCode) => {
    if (!activeScanOrder) return;
    try {
      await ordersApi.scanConfirm(activeScanOrder.id, scannedCode);
      toast.success(`Picking successfully confirmed for ${activeScanOrder.sku}!`);
      setActiveScanOrder(null);
      fetchOrdersData();
    } catch (err) {
      console.error(err);
      toast.error(err.response?.data?.message || 'Error validating pick (check location or available stock)');
    }
  };

  // Cancel Picking Task
  const handleCancelOrder = async (orderId) => {
    if (!window.confirm('Are you sure you want to cancel this picking task?')) return;
    try {
      await ordersApi.cancel(orderId);
      toast.success('Picking task cancelled.');
      fetchOrdersData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error cancelling task');
    }
  };

  const pendingOrders = orders.filter((o) => o.status === 'PENDING');

  return (
    <div className="space-y-8 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-16 animate-fade-in">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 glass-panel p-6 rounded-3xl border border-slate-800">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight flex items-center gap-2.5">
            <ListOrdered className="w-8 h-8 text-brand-400" />
            Picking List &bull; Outbound Logistics
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Optimized collection routing and instant validation via QR scanning
          </p>
        </div>

        <div className="flex items-center gap-3">
          <span className="px-3.5 py-1.5 rounded-full text-xs font-bold bg-brand-500/10 text-brand-400 border border-brand-500/20 font-mono">
            {pendingOrders.length} active tasks
          </span>
          <button
            onClick={fetchOrdersData}
            className="p-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-brand-400' : ''}`} />
          </button>
        </div>
      </div>

      {/* Active Picking Tasks Table */}
      <div className="glass-card rounded-3xl border border-slate-800 overflow-hidden shadow-2xl">
        <div className="p-6 border-b border-slate-800/80 bg-slate-900/40 flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <PackageCheck className="w-5 h-5 text-amber-400" />
              Pending Collection Tasks (Sorted by Optimal Route)
            </h3>
            <p className="text-xs text-slate-400 mt-0.5">Scan the QR code on the rack to confirm pick</p>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-900/80 text-[11px] font-bold uppercase tracking-wider text-slate-400 border-b border-slate-800">
              <tr>
                <th className="px-6 py-4">Task ID</th>
                <th className="px-6 py-4">Product SKU Code</th>
                <th className="px-6 py-4 text-center">Quantity</th>
                <th className="px-6 py-4">Rack Location</th>
                <th className="px-6 py-4 text-center">Status</th>
                <th className="px-6 py-4 text-right">Validation / Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-medium">
              {pendingOrders.map((order) => (
                <tr key={order.id} className="hover:bg-slate-800/40 transition-colors">
                  <td className="px-6 py-4 font-mono text-slate-400">#{order.id}</td>

                  <td className="px-6 py-4">
                    <span className="font-mono font-bold text-white text-base">{order.sku}</span>
                  </td>

                  <td className="px-6 py-4 text-center">
                    <span className="px-3 py-1 rounded-full text-xs font-bold bg-slate-800 text-slate-200 border border-slate-700">
                      {order.requestedQuantity} units
                    </span>
                  </td>

                  <td className="px-6 py-4">
                    {order.suggestedLocationCode ? (
                      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl text-xs font-mono font-bold bg-brand-500/10 text-brand-400 border border-brand-500/20">
                        <MapPin className="w-3.5 h-3.5" />
                        {order.suggestedLocationCode}
                      </span>
                    ) : (
                      <span className="text-xs text-slate-500">-</span>
                    )}
                  </td>

                  <td className="px-6 py-4 text-center">
                    <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20">
                      PENDING
                    </span>
                  </td>

                  <td className="px-6 py-4 text-right">
                    {!isViewer ? (
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => handleOpenScanner(order)}
                          className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-xs font-bold shadow-lg shadow-brand-600/30 transition-all hover:scale-105"
                        >
                          <QrCode className="w-4 h-4" />
                          <span>QR Validation</span>
                        </button>

                        <button
                          onClick={() => handleCancelOrder(order.id)}
                          title="Cancel task"
                          className="p-2 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 transition-colors"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    ) : (
                      <span className="text-xs text-slate-500 flex items-center justify-end gap-1 font-medium">
                        <Eye className="w-3.5 h-3.5" /> Read-Only
                      </span>
                    )}
                  </td>
                </tr>
              ))}

              {pendingOrders.length === 0 && (
                <tr>
                  <td colSpan="6" className="px-6 py-12 text-center text-slate-500">
                    <PackageCheck className="w-12 h-12 mx-auto text-slate-600 mb-2 opacity-50" />
                    <span>No pending picking tasks. All orders fulfilled!</span>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Recent Picking Validations History */}
      <div className="glass-card rounded-3xl border border-slate-800 overflow-hidden shadow-2xl">
        <div className="p-6 border-b border-slate-800/80 bg-slate-900/40">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <Clock className="w-5 h-5 text-indigo-400" />
            Recent Validations & Picking History
          </h3>
          <p className="text-xs text-slate-400 mt-0.5">Audit trail records for completed pick tasks</p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-900/80 text-[10px] font-bold uppercase tracking-wider text-slate-400 border-b border-slate-800">
              <tr>
                <th className="px-6 py-3.5">Date / Time</th>
                <th className="px-6 py-3.5">SKU Code & Product</th>
                <th className="px-6 py-3.5 text-center">Quantity</th>
                <th className="px-6 py-3.5">Operator</th>
                <th className="px-6 py-3.5 text-right">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-medium">
              {history.map((h) => (
                <tr key={h.id} className="hover:bg-slate-800/30">
                  <td className="px-6 py-3.5 font-mono text-slate-400">
                    {h.timestamp ? new Date(h.timestamp).toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''}
                  </td>
                  <td className="px-6 py-3.5">
                    <span className="font-mono font-bold text-white text-sm">{h.sku}</span>
                    <span className="text-slate-400 ml-2">({h.productName})</span>
                  </td>
                  <td className="px-6 py-3.5 text-center">
                    <span className="px-2.5 py-0.5 rounded-full font-mono font-bold bg-slate-800 text-slate-200">
                      -{Math.abs(h.quantityChanged)} units
                    </span>
                  </td>
                  <td className="px-6 py-3.5 font-bold text-brand-400">
                    {h.performedBy}
                  </td>
                  <td className="px-6 py-3.5 text-right">
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-[10px]">
                      <CheckCircle2 className="w-3 h-3" /> COMPLETED
                    </span>
                  </td>
                </tr>
              ))}

              {history.length === 0 && (
                <tr>
                  <td colSpan="5" className="px-6 py-8 text-center text-slate-500">
                    No picking activity recorded recently.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* QR Scanner Modal */}
      {activeScanOrder && (
        <QrScannerModal
          isOpen={Boolean(activeScanOrder)}
          onClose={() => setActiveScanOrder(null)}
          onScanSuccess={handleScanSuccess}
          title={`Picking Validation for SKU: ${activeScanOrder.sku}`}
          targetCode={activeScanOrder.suggestedLocationCode}
        />
      )}
    </div>
  );
}
