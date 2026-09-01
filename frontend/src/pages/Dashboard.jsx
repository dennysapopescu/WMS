import React, { useState, useEffect, useCallback, useId } from 'react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { useCurrency } from '../context/CurrencyContext';
import { dashboardApi } from '../api/dashboard';
import { productsApi } from '../api/products';
import { locationsApi } from '../api/locations';
import { ordersApi } from '../api/orders';
import { formatLocationDescription, formatAction } from '../utils/formatUtils';
import StatCard from '../components/StatCard';
import Modal from '../components/Modal';
import QrScannerModal from '../components/QrScannerModal';
import {
  Package,
  Boxes,
  AlertTriangle,
  Coins,
  Send,
  PlusCircle,
  Sparkles,
  ArrowRightLeft,
  Edit,
  Trash2,
  MinusCircle,
  FileDown,
  FileUp,
  Search,
  QrCode,
  History,
  ShieldAlert,
  Cpu,
  RefreshCw,
  Camera,
  Layers,
  ArrowDownCircle,
  UserCheck
} from 'lucide-react';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';
import { Doughnut, Pie } from 'react-chartjs-2';

ChartJS.register(ArcElement, Tooltip, Legend);

export default function Dashboard() {
  const { user, isAdmin, isOperator, isViewer } = useAuth();
  const { currency, formatPrice, formatValueOnly } = useCurrency();
  const toast = useToast();

  const [loading, setLoading] = useState(true);


  const [dashboardData, setDashboardData] = useState(null);
  const [products, setProducts] = useState([]);
  const [locations, setLocations] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [highlightedSku, setHighlightedSku] = useState(null);

  // Inbound Form State
  const [inboundName, setInboundName] = useState('');
  const [inboundSku, setInboundSku] = useState('');
  const [inboundQty, setInboundQty] = useState(1);
  const [inboundPrice, setInboundPrice] = useState(10.0);
  const [inboundLocationId, setInboundLocationId] = useState('');
  const [inboundLoading, setInboundLoading] = useState(false);

  // Outbound Picking Form State
  const [pickingSku, setPickingSku] = useState('');
  const [pickingQty, setPickingQty] = useState(1);
  const [pickingLoading, setPickingLoading] = useState(false);

  // Modals
  const [editProduct, setEditProduct] = useState(null);
  const [transferProduct, setTransferProduct] = useState(null);
  const [transferNewLocId, setTransferNewLocId] = useState('');
  const [qrModalImage, setQrModalImage] = useState(null);
  const [isScannerOpen, setIsScannerOpen] = useState(false);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [dash, prods, locs] = await Promise.all([
        dashboardApi.getSummary(),
        productsApi.getAll(),
        locationsApi.getAll(),
      ]);
      setDashboardData(dash);
      setProducts(prods);
      setLocations(locs);
    } catch (err) {
      console.error(err);
      toast.error('Failed to load warehouse data');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Smart Slotting Suggestion
  const handleSmartSuggestion = async () => {
    if (!inboundQty || inboundQty <= 0) {
      toast.warning('Please enter a valid quantity to calculate suggestion!');
      return;
    }
    try {
      const suggestion = await locationsApi.suggest(inboundQty);
      if (suggestion && suggestion.id) {
        setInboundLocationId(suggestion.id);
        toast.success(`🤖 AI Suggestion: Location ${suggestion.code} (${suggestion.availableCapacity} available spaces)`);
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'No location has sufficient capacity!');
    }
  };

  // Inbound Product Add
  const handleAddProduct = async (e) => {
    e.preventDefault();
    if (!inboundName || !inboundSku || !inboundLocationId) {
      toast.warning('Please fill in all required fields!');
      return;
    }
    setInboundLoading(true);
    try {
      await productsApi.create({
        name: inboundName,
        sku: inboundSku,
        quantity: parseInt(inboundQty, 10),
        price: parseFloat(inboundPrice),
        locationId: parseInt(inboundLocationId, 10),
      });
      toast.success(`Product "${inboundName}" registered successfully!`);
      setInboundName('');
      setInboundSku('');
      setInboundQty(1);
      setInboundPrice(10.0);
      setInboundLocationId('');
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error registering product');
    } finally {
      setInboundLoading(false);
    }
  };

  // Outbound Picking Task Create
  const handleCreatePicking = async (e) => {
    e.preventDefault();
    if (!pickingSku.trim()) {
      toast.warning('Please enter a SKU code!');
      return;
    }
    setPickingLoading(true);
    try {
      await ordersApi.create(pickingSku.trim(), parseInt(pickingQty, 10));
      toast.success(`Picking task generated for SKU: ${pickingSku}`);
      setPickingSku('');
      setPickingQty(1);
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error creating picking order');
    } finally {
      setPickingLoading(false);
    }
  };

  // Reduce Stock by 1
  const handleReduceStock = async (p) => {
    try {
      await productsApi.reduceQuantity(p.id);
      toast.success(`Stock reduced for ${p.sku} (-1 unit)`);
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error reducing stock');
    }
  };

  // Delete Product
  const handleDeleteProduct = async (p) => {
    if (!window.confirm(`Are you sure you want to delete product "${p.name}" (${p.sku})?`)) return;
    try {
      await productsApi.delete(p.id);
      toast.success(`Product "${p.name}" was deleted`);
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error deleting product');
    }
  };

  // Edit Product Submit
  const handleUpdateProduct = async (e) => {
    e.preventDefault();
    try {
      await productsApi.update(editProduct.id, {
        name: editProduct.name,
        sku: editProduct.sku,
        quantity: parseInt(editProduct.quantity, 10),
        price: parseFloat(editProduct.price),
        locationId: parseInt(editProduct.locationId, 10),
      });
      toast.success('Product updated successfully!');
      setEditProduct(null);
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error updating product');
    }
  };

  // Transfer Product Submit
  const handleTransferProduct = async (e) => {
    e.preventDefault();
    if (!transferNewLocId) {
      toast.warning('Please select the new destination location!');
      return;
    }
    try {
      await productsApi.transfer(transferProduct.id, parseInt(transferNewLocId, 10));
      toast.success(`Product ${transferProduct.sku} was transferred successfully!`);
      setTransferProduct(null);
      setTransferNewLocId('');
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error transferring product');
    }
  };

  // Import CSV
  const handleCsvImport = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const res = await productsApi.importCsv(file);
      toast.success(res.message || 'CSV Import completed!');
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Error importing CSV file');
    } finally {
      e.target.value = '';
    }
  };

  // On Quick QR Scan from Camera
  const handleQuickScan = (scanned) => {
    setSearchQuery(scanned);
    setHighlightedSku(scanned);
    toast.success(`Scanned successfully: ${scanned}`);
    setTimeout(() => setHighlightedSku(null), 4000);
  };

  // Filtered products list
  const filteredProducts = products.filter((p) => {
    const q = searchQuery.toLowerCase().trim();
    if (!q) return true;
    return (
      (p.name && p.name.toLowerCase().includes(q)) ||
      (p.sku && p.sku.toLowerCase().includes(q)) ||
      (p.locationCode && p.locationCode.toLowerCase().includes(q))
    );
  });

  // Chart Data Preparation
  const chartLabels = Object.keys(dashboardData?.stockDistribution || {});
  const chartValues = Object.values(dashboardData?.stockDistribution || {});
  const doughnutData = {
    labels: chartLabels.length ? chartLabels : ['No products'],
    datasets: [
      {
        data: chartValues.length ? chartValues : [1],
        backgroundColor: [
          '#0c87eb',
          '#10b981',
          '#f59e0b',
          '#8b5cf6',
          '#ec4899',
          '#06b6d4',
          '#f43f5e',
          '#84cc16',
        ],
        borderWidth: 2,
        borderColor: '#0f172a',
      },
    ],
  };

  return (
    <div className="space-y-8 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-16 animate-fade-in">
      {/* Top Banner & Refresh */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 glass-panel p-6 rounded-3xl border border-slate-800">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight flex items-center gap-2.5">
            <Boxes className="w-8 h-8 text-brand-400" />
            Executive Warehouse Dashboard
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Real-time inventory management, inbound/outbound flows, and AI slotting optimization
          </p>
        </div>

        <div className="flex items-center gap-3">
          {isViewer && (
            <span className="px-3.5 py-1.5 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              Viewer Mode (Read-Only)
            </span>
          )}
          <button
            onClick={loadData}
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-semibold border border-slate-700 transition-all hover:scale-105"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-brand-400' : ''}`} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <StatCard
          title="Unique Products"
          value={dashboardData?.totalItems ?? 0}
          unit="SKU"
          subtitle="Active catalog items"
          icon={Package}
          color="blue"
        />
        <StatCard
          title="Units in Stock"
          value={dashboardData?.totalQuantity ?? 0}
          unit="units"
          subtitle="Total inventoried stock"
          icon={Layers}
          color="emerald"
        />
        <StatCard
          title="Low Stock Alert"
          value={dashboardData?.lowStock ?? 0}
          unit="SKU"
          subtitle="Quantity ≤ 5 units"
          icon={AlertTriangle}
          color="rose"
        />
        <StatCard
          title="Inventory Value"
          value={dashboardData ? formatValueOnly(dashboardData.totalValue) : '0.00'}
          unit={currency.symbol}
          subtitle={`Valuation in ${currency.code}`}
          icon={Coins}
          color="amber"
        />


      </div>

      {/* Quick Inbound & Outbound Action Rows (Admin/Operator) */}
      {!isViewer && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Inbound Goods Registration (Col 8) */}
          <div className="lg:col-span-7 glass-card p-6 rounded-3xl border border-slate-800">
            <div className="flex items-center justify-between pb-4 mb-4 border-b border-slate-800">
              <div className="flex items-center gap-2.5">
                <div className="p-2 rounded-xl bg-brand-500/10 text-brand-400 border border-brand-500/20">
                  <ArrowDownCircle className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white">Inbound Goods Registration</h3>
                  <p className="text-xs text-slate-400">Receive inventory and smart rack allocation</p>
                </div>
              </div>
            </div>

            <form onSubmit={handleAddProduct} className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                  Product Name *
                </label>
                <input
                  type="text"
                  value={inboundName}
                  onChange={(e) => setInboundName(e.target.value)}
                  placeholder="e.g. Dell XPS 15 Laptop"
                  required
                  className="w-full px-3.5 py-2.5 bg-slate-900/90 border border-slate-800 focus:border-brand-500 rounded-xl text-sm text-white placeholder-slate-500 outline-none transition-colors"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                  SKU Code *
                </label>
                <input
                  type="text"
                  value={inboundSku}
                  onChange={(e) => setInboundSku(e.target.value)}
                  placeholder="e.g. SKU-LAPTOP-01"
                  required
                  className="w-full px-3.5 py-2.5 bg-slate-900/90 border border-slate-800 focus:border-brand-500 rounded-xl text-sm font-mono text-white placeholder-slate-500 outline-none transition-colors"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                  Received Quantity *
                </label>
                <input
                  type="number"
                  min="1"
                  value={inboundQty}
                  onChange={(e) => setInboundQty(e.target.value)}
                  required
                  className="w-full px-3.5 py-2.5 bg-slate-900/90 border border-slate-800 focus:border-brand-500 rounded-xl text-sm text-white outline-none transition-colors"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                  Unit Price ({currency.symbol}) *
                </label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={inboundPrice}
                  onChange={(e) => setInboundPrice(e.target.value)}
                  required
                  className="w-full px-3.5 py-2.5 bg-slate-900/90 border border-slate-800 focus:border-brand-500 rounded-xl text-sm text-white outline-none transition-colors"
                />
              </div>


              <div className="sm:col-span-2">
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                    Location / Rack Allocation *
                  </label>
                  <button
                    type="button"
                    onClick={handleSmartSuggestion}
                    className="flex items-center gap-1 text-xs font-bold text-brand-400 hover:text-brand-300 transition-colors"
                  >
                    <Sparkles className="w-3.5 h-3.5 text-amber-400" />
                    <span>Smart AI Slotting Suggestion</span>
                  </button>
                </div>
                <select
                  value={inboundLocationId}
                  onChange={(e) => setInboundLocationId(e.target.value)}
                  required
                  className="w-full px-3.5 py-2.5 bg-slate-900/90 border border-slate-800 focus:border-brand-500 rounded-xl text-sm text-white outline-none transition-colors"
                >
                  <option value="" disabled>Select a warehouse rack...</option>
                  {locations.map((loc) => (
                    <option key={loc.id} value={loc.id}>
                      {loc.code} - {formatLocationDescription(loc.description)} (Occupied: {loc.currentOccupancy}/{loc.maxCapacity})
                    </option>
                  ))}

                </select>
              </div>

              <div className="sm:col-span-2 pt-2">
                <button
                  type="submit"
                  disabled={inboundLoading}
                  className="w-full py-3 bg-brand-600 hover:bg-brand-500 text-white font-bold rounded-xl shadow-lg shadow-brand-600/30 transition-all flex items-center justify-center gap-2 text-sm disabled:opacity-50"
                >
                  <PlusCircle className="w-4 h-4" />
                  <span>{inboundLoading ? 'Processing...' : 'PROCESS INBOUND STOCK'}</span>
                </button>
              </div>
            </form>
          </div>

          {/* Outbound Picking Task Generator (Col 5) */}
          <div className="lg:col-span-5 flex flex-col justify-between glass-card p-6 rounded-3xl border border-slate-800">
            <div>
              <div className="flex items-center gap-2.5 pb-4 mb-4 border-b border-slate-800">
                <div className="p-2 rounded-xl bg-amber-500/10 text-amber-400 border border-amber-500/20">
                  <Send className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white">Generate Outbound Order (Picking)</h3>
                  <p className="text-xs text-slate-400">Create warehouse item collection task</p>
                </div>
              </div>

              <form onSubmit={handleCreatePicking} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                    Product SKU Code *
                  </label>
                  <input
                    type="text"
                    value={pickingSku}
                    onChange={(e) => setPickingSku(e.target.value)}
                    placeholder="e.g. SKU-123..."
                    required
                    className="w-full px-3.5 py-2.5 bg-slate-900/90 border border-slate-800 focus:border-amber-500 rounded-xl text-sm font-mono text-white placeholder-slate-500 outline-none transition-colors"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                    Requested Quantity *
                  </label>
                  <input
                    type="number"
                    min="1"
                    value={pickingQty}
                    onChange={(e) => setPickingQty(e.target.value)}
                    required
                    className="w-full px-3.5 py-2.5 bg-slate-900/90 border border-slate-800 focus:border-amber-500 rounded-xl text-sm text-white outline-none transition-colors"
                  />
                </div>

                <button
                  type="submit"
                  disabled={pickingLoading}
                  className="w-full py-3 bg-gradient-to-r from-amber-600 to-orange-600 hover:from-amber-500 hover:to-orange-500 text-white font-bold rounded-xl shadow-lg shadow-amber-600/30 transition-all flex items-center justify-center gap-2 text-sm disabled:opacity-50"
                >
                  <Send className="w-4 h-4" />
                  <span>{pickingLoading ? 'Generating...' : 'GENERATE PICKING TASK'}</span>
                </button>
              </form>
            </div>

            {/* Quick QR Scanner Trigger Button */}
            <div className="mt-6 pt-4 border-t border-slate-800">
              <button
                type="button"
                onClick={() => setIsScannerOpen(true)}
                className="w-full py-2.5 px-4 bg-slate-900 hover:bg-slate-800 border border-slate-700 hover:border-brand-500/50 rounded-xl text-xs font-bold text-brand-300 flex items-center justify-center gap-2 transition-all group"
              >
                <Camera className="w-4 h-4 text-brand-400 group-hover:scale-110 transition-transform" />
                <span>Open Fast QR Scanner Terminal</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Products Inventory Table */}
      <div className="glass-card rounded-3xl border border-slate-800 overflow-hidden shadow-2xl">
        {/* Table Header Controls */}
        <div className="p-6 border-b border-slate-800/80 flex flex-col md:flex-row md:items-center justify-between gap-4 bg-slate-900/40">
          <div className="flex items-center gap-3 flex-wrap">
            <h3 className="text-lg font-bold text-white flex items-center gap-2">
              <Boxes className="w-5 h-5 text-brand-400" />
              Stock & Product Catalog
            </h3>
            <span className="px-3 py-1 rounded-full text-xs font-bold bg-slate-800 text-slate-300 border border-slate-700">
              {filteredProducts.length} items
            </span>

            {/* Actions: Export PDF, Import CSV, Template */}
            <div className="flex items-center gap-2">
              <button
                onClick={productsApi.exportPdf}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/20 text-xs font-bold transition-colors"
              >
                <FileDown className="w-3.5 h-3.5" /> PDF
              </button>

              {!isViewer && (
                <>
                  <label className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/20 text-xs font-bold cursor-pointer transition-colors">
                    <FileUp className="w-3.5 h-3.5" /> Import CSV
                    <input type="file" accept=".csv" onChange={handleCsvImport} className="hidden" />
                  </label>

                  <button
                    onClick={productsApi.downloadTemplate}
                    title="Download CSV sample template"
                    className="p-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white border border-slate-700 transition-colors"
                  >
                    <FileDown className="w-3.5 h-3.5" />
                  </button>
                </>
              )}
            </div>
          </div>

          {/* Search bar */}
          <div className="relative w-full md:w-72">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search name, SKU, location..."
              className="w-full pl-10 pr-4 py-2 bg-slate-900 border border-slate-800 focus:border-brand-500 rounded-xl text-xs font-medium text-white placeholder-slate-500 outline-none transition-colors"
            />
          </div>
        </div>

        {/* Responsive Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-900/80 text-[11px] font-bold uppercase tracking-wider text-slate-400 border-b border-slate-800">
              <tr>
                <th className="px-6 py-4">Product & SKU</th>
                <th className="px-6 py-4">Rack Location</th>
                <th className="px-6 py-4 text-center">Quantity</th>
                <th className="px-6 py-4 text-right">Unit Price</th>
                <th className="px-6 py-4 text-right">Total Value</th>

                <th className="px-6 py-4 text-center">QR Code</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-medium">
              {filteredProducts.map((p) => {
                const isHighlighted = highlightedSku && p.sku?.toUpperCase() === highlightedSku.toUpperCase();
                return (
                  <tr
                    key={p.id}
                    className={`transition-colors hover:bg-slate-800/40 ${
                      isHighlighted ? 'bg-brand-500/20 border-l-4 border-l-brand-400 animate-pulse' : ''
                    }`}
                  >
                    {/* Name & SKU */}
                    <td className="px-6 py-4">
                      <div className="font-semibold text-white">{p.name}</div>
                      <div className="text-xs font-mono font-bold text-brand-400">{p.sku}</div>
                    </td>

                    {/* Location */}
                    <td className="px-6 py-4">
                      {p.locationCode ? (
                        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl text-xs font-mono font-bold bg-slate-800 text-slate-200 border border-slate-700">
                          {p.locationCode}
                        </span>
                      ) : (
                        <span className="text-xs text-slate-500">Unassigned</span>
                      )}
                    </td>

                    {/* Quantity */}
                    <td className="px-6 py-4 text-center">
                      <span
                        className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-bold ${
                          p.quantity <= 5
                            ? 'bg-rose-500/10 text-rose-400 border border-rose-500/30 animate-pulse'
                            : 'bg-slate-800 text-slate-200 border border-slate-700'
                        }`}
                      >
                        {p.quantity} units
                      </span>
                    </td>

                    {/* Price */}
                    <td className="px-6 py-4 text-right font-mono text-slate-300">
                      {formatPrice(p.price)}
                    </td>

                    {/* Total Value */}
                    <td className="px-6 py-4 text-right font-mono font-bold text-white">
                      {formatPrice(Number(p.price || 0) * Number(p.quantity || 0))}
                    </td>



                    {/* QR Code thumbnail */}
                    <td className="px-6 py-4 text-center">
                      <img
                        src={productsApi.getQrUrl(p.sku)}
                        alt={`QR ${p.sku}`}
                        onClick={() => setQrModalImage({ url: productsApi.getQrUrl(p.sku), title: p.name, sku: p.sku })}
                        className="w-9 h-9 mx-auto rounded-lg border border-slate-700 bg-white p-0.5 cursor-pointer hover:scale-125 transition-transform duration-200 shadow-md"
                        title="Click to view enlarged QR code"
                      />
                    </td>

                    {/* Actions */}
                    <td className="px-6 py-4 text-right">
                      {!isViewer ? (
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            onClick={() => setEditProduct({ ...p })}
                            title="Edit product"
                            className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-slate-700 transition-colors"
                          >
                            <Edit className="w-3.5 h-3.5" />
                          </button>

                          <button
                            onClick={() => handleReduceStock(p)}
                            title="Decrease quantity (-1)"
                            className="p-2 rounded-xl bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 border border-amber-500/20 transition-colors"
                          >
                            <MinusCircle className="w-3.5 h-3.5" />
                          </button>

                          <button
                            onClick={() => setTransferProduct(p)}
                            title="Transfer to another rack"
                            className="p-2 rounded-xl bg-brand-500/10 hover:bg-brand-500/20 text-brand-400 border border-brand-500/20 transition-colors"
                          >
                            <ArrowRightLeft className="w-3.5 h-3.5" />
                          </button>

                          {isAdmin && (
                            <button
                              onClick={() => handleDeleteProduct(p)}
                              title="Delete product"
                              className="p-2 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/20 transition-colors"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          )}
                        </div>
                      ) : (
                        <span className="text-xs text-slate-500 font-medium">Read-Only</span>
                      )}
                    </td>
                  </tr>
                );
              })}

              {filteredProducts.length === 0 && (
                <tr>
                  <td colSpan="7" className="px-6 py-12 text-center text-slate-500">
                    <Boxes className="w-12 h-12 mx-auto text-slate-600 mb-2 opacity-50" />
                    <span>No products found matching your search.</span>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Analytics & Predictive Restocking Section */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Chart Distribution (Col 6) */}
        <div className="lg:col-span-6 glass-card p-6 rounded-3xl border border-slate-800 flex flex-col justify-between">
          <div>
            <h3 className="text-base font-bold text-white flex items-center gap-2 mb-1">
              <Layers className="w-5 h-5 text-brand-400" />
              Stock Distribution by Location
            </h3>
            <p className="text-xs text-slate-400 mb-6">Distinct SKU count stored across each sector</p>
          </div>

          <div className="w-full max-w-xs mx-auto py-2">
            <Doughnut
              data={doughnutData}
              options={{
                responsive: true,
                plugins: {
                  legend: {
                    position: 'bottom',
                    labels: { color: '#94a3b8', font: { size: 11, weight: 600 } },
                  },
                },
                cutout: '68%',
              }}
            />
          </div>
        </div>

        {/* Predictive Restocking & AI Security (Col 6) */}
        <div className="lg:col-span-6 space-y-6">
          {/* Predictive Restocking */}
          <div className="glass-card p-6 rounded-3xl border border-slate-800">
            <div className="flex items-center gap-2 mb-1">
              <Cpu className="w-5 h-5 text-indigo-400" />
              <h3 className="text-base font-bold text-white">Predictive Restocking (AI Burn Rate)</h3>
            </div>
            <p className="text-xs text-slate-400 mb-4">
              Estimated stock depletion timeframe based on last 7 days picking velocity
            </p>

            <div className="space-y-2.5 max-h-56 overflow-y-auto pr-1">
              {products.filter((p) => dashboardData?.predictions?.[p.sku] && dashboardData?.predictions?.[p.sku] !== 'Stable').length > 0 ? (
                products
                  .filter((p) => dashboardData?.predictions?.[p.sku] && dashboardData?.predictions?.[p.sku] !== 'Stable')
                  .map((p) => (

                    <div
                      key={p.id}
                      className="p-3 rounded-2xl bg-slate-900/80 border border-slate-800 flex items-center justify-between gap-3"
                    >
                      <div>
                        <span className="text-sm font-semibold text-white block">{p.name}</span>
                        <span className="text-xs font-mono text-slate-400">{p.sku} &bull; Stock: {p.quantity} units</span>
                      </div>
                      <span className="px-3 py-1 rounded-full text-xs font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20">
                        {dashboardData?.predictions?.[p.sku]}
                      </span>
                    </div>
                  ))
              ) : (
                <div className="p-4 text-center text-xs text-slate-500 bg-slate-900/50 rounded-2xl border border-slate-800">
                  Current stock levels are stable based on recent picking velocity.
                </div>
              )}
            </div>
          </div>

          {/* AI Security Audit Anomaly Alert */}
          {isAdmin && (
            <div className="glass-card p-6 rounded-3xl border border-rose-500/20 bg-rose-950/20">
              <div className="flex items-center gap-2 mb-1">
                <ShieldAlert className="w-5 h-5 text-rose-400" />
                <h3 className="text-base font-bold text-white">AI Security Audit (Anomaly Detection)</h3>
              </div>
              <p className="text-xs text-slate-400 mb-4">
                Automated overnight activity monitoring (22:00 - 06:00)
              </p>

              <div className="space-y-2">
                {dashboardData?.aiAlerts && dashboardData.aiAlerts.length > 0 ? (
                  dashboardData.aiAlerts.map((alert, idx) => (
                    <div
                      key={idx}
                      className="p-2.5 rounded-xl bg-rose-500/10 border border-rose-500/20 text-xs font-medium text-rose-300 flex items-center gap-2"
                    >
                      <AlertTriangle className="w-4 h-4 shrink-0 text-rose-400" />
                      <span>{alert}</span>
                    </div>
                  ))
                ) : (
                  <div className="p-3 text-center text-xs font-semibold text-emerald-400 bg-emerald-500/10 rounded-xl border border-emerald-500/20">
                    ✓ No security anomalies detected. System integrity verified.
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Activity Logs Section */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* My Activity (Col 5) */}
        {!isViewer && (
          <div className="lg:col-span-5 glass-card p-6 rounded-3xl border border-slate-800">
            <h3 className="text-base font-bold text-white flex items-center gap-2 mb-1">
              <UserCheck className="w-5 h-5 text-brand-400" />
              My Recent Activity
            </h3>
            <p className="text-xs text-slate-400 mb-4">Latest actions registered by your account</p>

            <div className="space-y-2.5">
              {dashboardData?.myActivity && dashboardData.myActivity.length > 0 ? (
                dashboardData.myActivity.map((log) => (
                  <div
                    key={log.id}
                    className="p-3 rounded-2xl bg-slate-900/60 border border-slate-800/80 flex items-center justify-between gap-3 text-xs"
                  >
                    <div>
                      <span className="font-bold text-slate-200 block">{formatAction(log.action)}</span>
                      <span className="text-slate-400">{log.productName} ({log.sku})</span>
                    </div>
                    <span className="font-mono text-slate-400 text-[11px]">
                      {log.timestamp ? new Date(log.timestamp).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }) : ''}
                    </span>
                  </div>
                ))
              ) : (
                <div className="p-4 text-center text-xs text-slate-500 bg-slate-900/50 rounded-2xl">
                  No recent activity.
                </div>
              )}
            </div>
          </div>
        )}

        {/* General Audit Trail (Col 7 / 12) */}
        <div className={`${!isViewer ? 'lg:col-span-7' : 'lg:col-span-12'} glass-card p-6 rounded-3xl border border-slate-800`}>
          <h3 className="text-base font-bold text-white flex items-center gap-2 mb-1">
            <History className="w-5 h-5 text-indigo-400" />
            Audit Trail - General Activity Log
          </h3>
          <p className="text-xs text-slate-400 mb-4">Complete traceability of stock movements</p>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-900/80 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                <tr>
                  <th className="px-4 py-2.5">Date / Time</th>
                  <th className="px-4 py-2.5">Product</th>
                  <th className="px-4 py-2.5">Action</th>
                  <th className="px-4 py-2.5 text-right">Operator</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {dashboardData?.recentLogs?.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-800/30">
                    <td className="px-4 py-2.5 font-mono text-slate-400">
                      {log.timestamp ? new Date(log.timestamp).toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''}
                    </td>
                    <td className="px-4 py-2.5 font-semibold text-slate-200">
                      {log.productName}
                    </td>
                    <td className="px-4 py-2.5">
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-brand-500/10 text-brand-300 border border-brand-500/20">
                        {formatAction(log.action)}
                      </span>
                    </td>

                    <td className="px-4 py-2.5 text-right font-bold text-brand-400">
                      {log.performedBy}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Edit Product Modal */}
      {editProduct && (
        <Modal
          isOpen={Boolean(editProduct)}
          onClose={() => setEditProduct(null)}
          title={`Edit Product: ${editProduct.name}`}
          icon={Edit}
        >
          <form onSubmit={handleUpdateProduct} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                Product Name
              </label>
              <input
                type="text"
                value={editProduct.name}
                onChange={(e) => setEditProduct({ ...editProduct, name: e.target.value })}
                required
                className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm text-white outline-none focus:border-brand-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                SKU Code
              </label>
              <input
                type="text"
                value={editProduct.sku}
                onChange={(e) => setEditProduct({ ...editProduct, sku: e.target.value })}
                required
                className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm font-mono text-white outline-none focus:border-brand-500"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                  Quantity
                </label>
                <input
                  type="number"
                  min="0"
                  value={editProduct.quantity}
                  onChange={(e) => setEditProduct({ ...editProduct, quantity: e.target.value })}
                  required
                  className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm text-white outline-none focus:border-brand-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                  Price ({currency.symbol})
                </label>
                <input


                  type="number"
                  step="0.01"
                  min="0"
                  value={editProduct.price}
                  onChange={(e) => setEditProduct({ ...editProduct, price: e.target.value })}
                  required
                  className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm text-white outline-none focus:border-brand-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                Location
              </label>
              <select
                value={editProduct.locationId || ''}
                onChange={(e) => setEditProduct({ ...editProduct, locationId: e.target.value })}
                required
                className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm text-white outline-none focus:border-brand-500"
              >
                {locations.map((loc) => (
                  <option key={loc.id} value={loc.id}>
                    {loc.code} - {formatLocationDescription(loc.description)}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
              <button
                type="button"
                onClick={() => setEditProduct(null)}
                className="px-4 py-2 rounded-xl text-sm font-semibold text-slate-400 hover:text-white"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-5 py-2 rounded-xl bg-brand-600 hover:bg-brand-500 text-white text-sm font-bold shadow-lg shadow-brand-600/30"
              >
                Save Changes
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Transfer Product Modal */}
      {transferProduct && (
        <Modal
          isOpen={Boolean(transferProduct)}
          onClose={() => setTransferProduct(null)}
          title={`Transfer Product: ${transferProduct.name}`}
          icon={ArrowRightLeft}
        >
          <form onSubmit={handleTransferProduct} className="space-y-4">
            <div className="p-3 bg-brand-500/10 border border-brand-500/20 rounded-xl text-xs text-brand-300">
              Product <strong className="text-white">{transferProduct.sku}</strong> (stock: {transferProduct.quantity} units) will be transferred to the selected rack, and occupancy will be recalculated automatically on the Digital Twin map.
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                New Destination *
              </label>
              <select
                value={transferNewLocId}
                onChange={(e) => setTransferNewLocId(e.target.value)}
                required
                className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm text-white outline-none focus:border-brand-500"
              >
                <option value="" disabled>Select new destination rack...</option>
                {locations
                  .filter((loc) => loc.id !== transferProduct.locationId)
                  .map((loc) => (
                    <option key={loc.id} value={loc.id}>
                      {loc.code} - {formatLocationDescription(loc.description)} (Free: {loc.availableCapacity} units)
                    </option>
                  ))}
              </select>
            </div>


            <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
              <button
                type="button"
                onClick={() => setTransferProduct(null)}
                className="px-4 py-2 rounded-xl text-sm font-semibold text-slate-400 hover:text-white"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-5 py-2 rounded-xl bg-amber-600 hover:bg-amber-500 text-white text-sm font-bold shadow-lg shadow-amber-600/30"
              >
                Confirm Transfer
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* QR Code Zoom Modal */}
      {qrModalImage && (
        <Modal
          isOpen={Boolean(qrModalImage)}
          onClose={() => setQrModalImage(null)}
          title={`QR Code: ${qrModalImage.title}`}
          icon={QrCode}
          size="sm"
        >
          <div className="text-center space-y-4">
            <div className="p-4 bg-white rounded-2xl inline-block shadow-xl">
              <img src={qrModalImage.url} alt="QR Zoom" className="w-56 h-56 mx-auto" />
            </div>
            <div className="font-mono text-sm font-bold text-brand-400">{qrModalImage.sku}</div>
            <button
              onClick={() => window.print()}
              className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white text-xs font-bold border border-slate-700 transition-colors"
            >
              Print Label
            </button>
          </div>
        </Modal>
      )}

      {/* QR Scanner Modal Terminal */}
      <QrScannerModal
        isOpen={isScannerOpen}
        onClose={() => setIsScannerOpen(false)}
        onScanSuccess={handleQuickScan}
        title="Fast QR Scanner Terminal (SKU / Location)"
      />
    </div>
  );
}
