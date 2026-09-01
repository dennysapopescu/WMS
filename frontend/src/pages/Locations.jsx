import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { locationsApi } from '../api/locations';
import { formatLocationDescription } from '../utils/formatUtils';
import Modal from '../components/Modal';

import {
  Grid3X3,
  Map,
  PlusCircle,
  Edit,
  Trash2,
  QrCode,
  Layers,
  ArrowRight,
  AlertCircle,
  CheckCircle2,
  RefreshCw
} from 'lucide-react';

export default function Locations() {
  const { isAdmin, isViewer } = useAuth();
  const toast = useToast();

  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(true);

  // Add Location Form
  const [newCode, setNewCode] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [newCapacity, setNewCapacity] = useState(100);
  const [addLoading, setAddLoading] = useState(false);

  // Edit Location Modal
  const [editLoc, setEditLoc] = useState(null);
  const [qrZoom, setQrZoom] = useState(null);

  const fetchLocations = useCallback(async () => {
    try {
      setLoading(true);
      const data = await locationsApi.getAll();
      setLocations(data);
    } catch (err) {
      toast.error('Failed to load warehouse locations');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    fetchLocations();
  }, [fetchLocations]);

  const handleAddLocation = async (e) => {
    e.preventDefault();
    if (!newCode.trim()) {
      toast.warning('Location code is required!');
      return;
    }
    setAddLoading(true);
    try {
      await locationsApi.create({
        code: newCode.trim().toUpperCase(),
        description: newDesc.trim(),
        maxCapacity: parseInt(newCapacity, 10),
      });
      toast.success(`Location ${newCode.toUpperCase()} added successfully!`);
      setNewCode('');
      setNewDesc('');
      setNewCapacity(100);
      fetchLocations();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error creating location');
    } finally {
      setAddLoading(false);
    }
  };

  const handleUpdateLocation = async (e) => {
    e.preventDefault();
    try {
      await locationsApi.update(editLoc.id, {
        code: editLoc.code.trim().toUpperCase(),
        description: editLoc.description,
        maxCapacity: parseInt(editLoc.maxCapacity, 10),
      });
      toast.success('Location updated successfully!');
      setEditLoc(null);
      fetchLocations();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error updating location');
    }
  };

  const handleDeleteLocation = async (loc) => {
    if (!window.confirm(`Are you sure you want to delete sector ${loc.code}? The system will block deletion if products are stored inside.`)) {
      return;
    }
    try {
      await locationsApi.delete(loc.id);
      toast.success(`Location ${loc.code} deleted`);
      fetchLocations();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Cannot delete a location that contains products!');
    }
  };

  return (
    <div className="space-y-8 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-16 animate-fade-in">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 glass-panel p-6 rounded-3xl border border-slate-800">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight flex items-center gap-2.5">
            <Grid3X3 className="w-8 h-8 text-brand-400" />
            Digital Twin &bull; Location Configuration
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Manage racks, maximum capacities, and generate localization QR codes
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Link
            to="/map"
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-sm font-bold shadow-lg shadow-brand-600/30 transition-all hover:scale-105"
          >
            <Map className="w-4 h-4" />
            <span>Open 2D Warehouse Map</span>
          </Link>
          <button
            onClick={fetchLocations}
            className="p-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-brand-400' : ''}`} />
          </button>
        </div>
      </div>

      {/* Add New Location (Admin only) */}
      {isAdmin && (
        <div className="glass-card p-6 rounded-3xl border border-slate-800">
          <div className="flex items-center gap-2.5 pb-4 mb-4 border-b border-slate-800">
            <div className="p-2 rounded-xl bg-brand-500/10 text-brand-400 border border-brand-500/20">
              <PlusCircle className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">Register New Sector / Rack</h3>
              <p className="text-xs text-slate-400">Add a new storage zone to the warehouse</p>
            </div>
          </div>

          <form onSubmit={handleAddLocation} className="grid grid-cols-1 sm:grid-cols-12 gap-4 items-end">
            <div className="sm:col-span-3">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                Identification Code (QR) *
              </label>
              <input
                type="text"
                value={newCode}
                onChange={(e) => setNewCode(e.target.value)}
                placeholder="e.g. R-01-A"
                required
                className="w-full px-3.5 py-2.5 bg-slate-900/90 border border-slate-800 focus:border-brand-500 rounded-xl text-sm font-mono text-white placeholder-slate-500 outline-none transition-colors"
              />
            </div>

            <div className="sm:col-span-4">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                Description / Category
              </label>
              <input
                type="text"
                value={newDesc}
                onChange={(e) => setNewDesc(e.target.value)}
                placeholder="e.g. Electronics / Fragile Goods"
                className="w-full px-3.5 py-2.5 bg-slate-900/90 border border-slate-800 focus:border-brand-500 rounded-xl text-sm text-white placeholder-slate-500 outline-none transition-colors"
              />
            </div>

            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                Max Capacity *
              </label>
              <input
                type="number"
                min="1"
                value={newCapacity}
                onChange={(e) => setNewCapacity(e.target.value)}
                required
                className="w-full px-3.5 py-2.5 bg-slate-900/90 border border-slate-800 focus:border-brand-500 rounded-xl text-sm text-white outline-none transition-colors"
              />
            </div>

            <div className="sm:col-span-3">
              <button
                type="submit"
                disabled={addLoading}
                className="w-full py-2.5 bg-brand-600 hover:bg-brand-500 text-white font-bold rounded-xl shadow-lg shadow-brand-600/30 transition-all flex items-center justify-center gap-2 text-sm disabled:opacity-50"
              >
                <PlusCircle className="w-4 h-4" />
                <span>{addLoading ? 'Adding...' : 'ADD TO SYSTEM'}</span>
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Locations Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {locations.map((loc) => {
          const occupancy = loc.currentOccupancy || 0;
          const max = loc.maxCapacity || 100;
          const percent = max > 0 ? Math.round((occupancy / max) * 100) : 0;
          const isFull = occupancy >= max;
          const isWarning = percent >= 80 && !isFull;

          return (
            <div
              key={loc.id}
              className="glass-card p-6 rounded-3xl border border-slate-800 hover:border-slate-700 transition-all duration-300 flex flex-col justify-between group"
            >
              {/* Header */}
              <div>
                <div className="flex items-start justify-between gap-4 mb-4">
                  <div>
                    <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold tracking-wider uppercase bg-brand-500/10 text-brand-400 border border-brand-500/20">
                      Rack / Sector
                    </span>
                    <h3 className="text-2xl font-extrabold font-mono text-white mt-1 group-hover:text-brand-400 transition-colors">
                      {loc.code}
                    </h3>
                    <p className="text-xs text-slate-400 mt-0.5">{formatLocationDescription(loc.description)}</p>
                  </div>

                  {/* QR code thumbnail */}
                  <img
                    src={locationsApi.getQrUrl(loc.code)}
                    alt={`QR ${loc.code}`}
                    onClick={() => setQrZoom({ url: locationsApi.getQrUrl(loc.code), code: loc.code, desc: formatLocationDescription(loc.description) })}
                    className="w-14 h-14 rounded-xl border border-slate-700 bg-white p-1 cursor-pointer hover:scale-110 transition-transform shadow-md shrink-0"
                    title="Click to enlarge location QR code"
                  />

                </div>
              </div>

              {/* Progress & Occupancy */}
              <div className="mt-6 pt-4 border-t border-slate-800/80">
                <div className="flex items-center justify-between text-xs font-semibold mb-2">
                  <span className="text-slate-400">Occupancy:</span>
                  <span className="text-white font-mono">{occupancy} / {max} units ({percent}%)</span>
                </div>

                {/* Animated Progress Bar */}
                <div className="w-full h-3 bg-slate-900 rounded-full overflow-hidden p-0.5 border border-slate-800">
                  <div
                    style={{ width: `${Math.min(100, percent)}%` }}
                    className={`h-full rounded-full transition-all duration-500 ${
                      isFull
                        ? 'bg-gradient-to-r from-rose-600 to-red-500 animate-pulse'
                        : isWarning
                        ? 'bg-gradient-to-r from-amber-500 to-orange-500'
                        : 'bg-gradient-to-r from-emerald-500 to-teal-400'
                    }`}
                  />
                </div>

                {/* Status Badge & Actions */}
                <div className="flex items-center justify-between mt-4">
                  <div>
                    {isFull ? (
                      <span className="px-2.5 py-1 rounded-full text-[11px] font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                        FULLY OCCUPIED
                      </span>
                    ) : (
                      <span className="px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        AVAILABLE ({loc.availableCapacity || (max - occupancy)} spaces)
                      </span>
                    )}
                  </div>

                  {isAdmin && (
                    <div className="flex items-center gap-1.5">
                      <button
                        onClick={() => setEditLoc({ ...loc })}
                        className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-colors"
                        title="Edit sector"
                      >
                        <Edit className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleDeleteLocation(loc)}
                        className="p-1.5 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 transition-colors"
                        title="Delete sector"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Edit Location Modal */}
      {editLoc && (
        <Modal
          isOpen={Boolean(editLoc)}
          onClose={() => setEditLoc(null)}
          title={`Edit Location: ${editLoc.code}`}
          icon={Edit}
        >
          <form onSubmit={handleUpdateLocation} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                Identification Code *
              </label>
              <input
                type="text"
                value={editLoc.code}
                onChange={(e) => setEditLoc({ ...editLoc, code: e.target.value })}
                required
                className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm font-mono text-white outline-none focus:border-brand-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                Description
              </label>
              <input
                type="text"
                value={editLoc.description || ''}
                onChange={(e) => setEditLoc({ ...editLoc, description: e.target.value })}
                className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm text-white outline-none focus:border-brand-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                Maximum Capacity *
              </label>
              <input
                type="number"
                min="1"
                value={editLoc.maxCapacity}
                onChange={(e) => setEditLoc({ ...editLoc, maxCapacity: e.target.value })}
                required
                className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm text-white outline-none focus:border-brand-500"
              />
            </div>

            <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
              <button
                type="button"
                onClick={() => setEditLoc(null)}
                className="px-4 py-2 rounded-xl text-sm font-semibold text-slate-400 hover:text-white"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-5 py-2 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-sm font-bold shadow-lg shadow-brand-600/30"
              >
                Save
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* QR Zoom Modal */}
      {qrZoom && (
        <Modal
          isOpen={Boolean(qrZoom)}
          onClose={() => setQrZoom(null)}
          title={`Location QR Code: ${qrZoom.code}`}
          icon={QrCode}
          size="sm"
        >
          <div className="text-center space-y-4">
            <div className="p-4 bg-white rounded-2xl inline-block shadow-xl">
              <img src={qrZoom.url} alt="QR Zoom" className="w-56 h-56 mx-auto" />
            </div>
            <div className="font-mono text-sm font-bold text-brand-400">{qrZoom.code}</div>
            <p className="text-xs text-slate-400">{qrZoom.desc || 'Scannable label for picking & inventory transfer'}</p>
            <button
              onClick={() => window.print()}
              className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white text-xs font-bold border border-slate-700 transition-colors"
            >
              Print Rack Label
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
