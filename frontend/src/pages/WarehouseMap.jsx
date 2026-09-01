import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useToast } from '../context/ToastContext';
import { locationsApi } from '../api/locations';
import { formatLocationDescription } from '../utils/formatUtils';
import Modal from '../components/Modal';

import {
  Map,
  ArrowLeft,
  RefreshCw,
  Package,
  Layers,
  Info,
  Boxes,
  AlertTriangle
} from 'lucide-react';

export default function WarehouseMap() {
  const toast = useToast();
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(true);

  // Inspector Modal State
  const [selectedLoc, setSelectedLoc] = useState(null);
  const [locProducts, setLocProducts] = useState([]);
  const [productsLoading, setProductsLoading] = useState(false);

  const fetchMapData = useCallback(async () => {
    try {
      setLoading(true);
      const data = await locationsApi.getAll();
      // Sort alphabetically by code for spatial map layout
      const sorted = [...data].sort((a, b) => a.code.localeCompare(b.code));
      setLocations(sorted);
    } catch (err) {
      toast.error('Failed to load warehouse map');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    fetchMapData();
  }, [fetchMapData]);

  const handleCellClick = async (loc) => {
    setSelectedLoc(loc);
    setProductsLoading(true);
    setLocProducts([]);

    try {
      const prods = await locationsApi.getProductsInLocation(loc.id);
      setLocProducts(prods || []);
    } catch (err) {
      toast.error('Failed to fetch rack contents');
    } finally {
      setProductsLoading(false);
    }
  };

  const getCellTheme = (current, max) => {
    const perc = max > 0 ? (current / max) * 100 : 0;
    if (perc < 30) {
      return {
        bg: 'from-emerald-600 to-emerald-700 hover:from-emerald-500 hover:to-emerald-600',
        border: 'border-emerald-500/40',
        badge: 'text-emerald-300 bg-emerald-950/60',
        glow: 'glow-emerald',
        pulse: false,
      };
    }
    if (perc < 80) {
      return {
        bg: 'from-amber-600 to-amber-700 hover:from-amber-500 hover:to-amber-600',
        border: 'border-amber-500/40',
        badge: 'text-amber-300 bg-amber-950/60',
        glow: 'glow-amber',
        pulse: false,
      };
    }
    return {
      bg: 'from-rose-600 to-rose-700 hover:from-rose-500 hover:to-rose-600',
      border: 'border-rose-500/40',
      badge: 'text-rose-300 bg-rose-950/60',
      glow: 'glow-rose',
      pulse: perc >= 90,
    };
  };

  return (
    <div className="space-y-8 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-16 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 glass-panel p-6 rounded-3xl border border-slate-800">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight flex items-center gap-2.5">
            <Map className="w-8 h-8 text-brand-400" />
            Digital Twin &bull; Interactive 2D Map
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Real-time spatial visualization of capacity occupancy and product contents per sector
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Link
            to="/"
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-semibold border border-slate-700 transition-all hover:scale-105"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Dashboard</span>
          </Link>
          <button
            onClick={fetchMapData}
            className="p-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-brand-400' : ''}`} />
          </button>
        </div>
      </div>

      {/* Legend & Summary Info */}
      <div className="glass-card p-5 rounded-2xl border border-slate-800 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-6 flex-wrap">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
            <Info className="w-4 h-4 text-brand-400" /> Heatmap Legend:
          </span>
          <div className="flex items-center gap-2 text-xs font-semibold text-slate-200">
            <span className="w-3.5 h-3.5 rounded-lg bg-emerald-500 shadow-sm" /> Available (&lt; 30%)
          </div>
          <div className="flex items-center gap-2 text-xs font-semibold text-slate-200">
            <span className="w-3.5 h-3.5 rounded-lg bg-amber-500 shadow-sm" /> Medium (30% - 80%)
          </div>
          <div className="flex items-center gap-2 text-xs font-semibold text-slate-200">
            <span className="w-3.5 h-3.5 rounded-lg bg-rose-500 shadow-sm" /> Crowded (&gt; 80%)
          </div>
          <div className="flex items-center gap-2 text-xs font-semibold text-rose-400">
            <span className="w-3.5 h-3.5 rounded-lg bg-rose-600 border border-white animate-ping" /> Critical Alert (&ge; 90%)
          </div>
        </div>

        <div className="text-xs font-mono text-slate-400">
          Total mapped sectors: <strong className="text-white">{locations.length}</strong>
        </div>
      </div>

      {/* 2D Interactive Warehouse Grid */}
      <div className="glass-card p-8 rounded-3xl border border-slate-800 shadow-2xl">
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 xl:grid-cols-8 gap-4">
          {locations.map((loc) => {
            const occupancy = loc.currentOccupancy || 0;
            const max = loc.maxCapacity || 100;
            const percent = max > 0 ? Math.round((occupancy / max) * 100) : 0;
            const theme = getCellTheme(occupancy, max);

            return (
              <button
                key={loc.id}
                type="button"
                onClick={() => handleCellClick(loc)}
                className={`aspect-square p-3 rounded-2xl bg-gradient-to-br ${theme.bg} border ${theme.border} ${theme.glow} shadow-xl flex flex-col items-center justify-between text-white transition-all duration-300 hover:scale-105 active:scale-95 group relative ${
                  theme.pulse ? 'ring-2 ring-rose-400 ring-offset-2 ring-offset-slate-950 animate-pulse' : ''
                }`}
              >
                {/* Sector Code */}
                <div className="w-full flex items-center justify-between">
                  <span className="text-[10px] uppercase font-bold text-white/80 tracking-wider">RACK</span>
                  <span className="text-xs font-bold text-white/90">{percent}%</span>
                </div>

                <div className="text-center my-auto">
                  <span className="text-lg font-mono font-black tracking-tight block drop-shadow-md">
                    {loc.code}
                  </span>
                  <span className="text-[10px] text-white/80 block line-clamp-1 max-w-[90px] mx-auto">
                    {formatLocationDescription(loc.description)}
                  </span>
                </div>

                {/* Units occupancy */}
                <div className="w-full text-center">
                  <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded-full ${theme.badge} shadow-inner`}>
                    {occupancy}/{max}
                  </span>
                </div>
              </button>
            );
          })}
        </div>

        {locations.length === 0 && !loading && (
          <div className="py-16 text-center text-slate-500">
            <Boxes className="w-16 h-16 mx-auto mb-3 opacity-30 text-slate-400" />
            <p className="text-sm font-semibold">No locations configured in the warehouse.</p>
          </div>
        )}
      </div>

      {/* Rack Inspector Modal */}
      {selectedLoc && (
        <Modal
          isOpen={Boolean(selectedLoc)}
          onClose={() => setSelectedLoc(null)}
          title={`Rack Inspection: ${selectedLoc.code} (${formatLocationDescription(selectedLoc.description)})`}
          icon={Package}
          size="lg"
        >

          <div className="space-y-5">
            {/* Occupancy summary header */}
            <div className="p-4 bg-slate-900/80 rounded-2xl border border-slate-800 flex items-center justify-between">
              <div>
                <span className="text-xs uppercase font-bold text-slate-400">Occupancy Rate</span>
                <div className="text-xl font-bold font-mono text-white mt-0.5">
                  {selectedLoc.currentOccupancy} / {selectedLoc.maxCapacity} units ({Math.round(((selectedLoc.currentOccupancy || 0) / (selectedLoc.maxCapacity || 1)) * 100)}%)
                </div>
              </div>
              <div>
                <span className="text-xs uppercase font-bold text-slate-400">Available Space</span>
                <div className="text-xl font-bold font-mono text-brand-400 mt-0.5">
                  {selectedLoc.availableCapacity || (selectedLoc.maxCapacity - selectedLoc.currentOccupancy)} units
                </div>
              </div>
            </div>

            {/* Products List in this Location */}
            <div>
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">
                Products Stored in This Sector
              </h4>

              {productsLoading ? (
                <div className="py-8 text-center text-slate-400 text-sm">
                  Loading items...
                </div>
              ) : locProducts.length > 0 ? (
                <div className="space-y-2.5 max-h-72 overflow-y-auto pr-1">
                  {locProducts.map((p) => (
                    <div
                      key={p.id}
                      className="p-3.5 rounded-xl bg-slate-900/90 border border-slate-800 flex items-center justify-between gap-4"
                    >
                      <div>
                        <div className="font-semibold text-white text-sm">{p.name}</div>
                        <div className="text-xs font-mono font-bold text-brand-400">{p.sku}</div>
                      </div>
                      <span className="px-3 py-1 rounded-full text-xs font-bold bg-brand-500/10 text-brand-300 border border-brand-500/20 font-mono">
                        {p.quantity} units
                      </span>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="py-8 text-center text-slate-500 bg-slate-900/40 rounded-2xl border border-slate-800">
                  <Package className="w-10 h-10 mx-auto text-slate-600 mb-2 opacity-50" />
                  <p className="text-sm font-semibold text-slate-400">This rack is completely empty.</p>
                </div>
              )}
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
