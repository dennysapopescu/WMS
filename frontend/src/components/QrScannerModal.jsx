import React, { useEffect, useRef, useState } from 'react';
import { Html5Qrcode } from 'html5-qrcode';
import { Camera, Upload, X, AlertCircle, CheckCircle2, RefreshCw } from 'lucide-react';
import Modal from './Modal';

export default function QrScannerModal({ isOpen, onClose, onScanSuccess, title = "QR Code Scanner", targetCode = null }) {
  const [activeTab, setActiveTab] = useState('camera'); // 'camera' | 'file'
  const [feedback, setFeedback] = useState(null);
  const [cameraError, setCameraError] = useState(null);
  const [isScanning, setIsScanning] = useState(false);
  const scannerRef = useRef(null);
  const readerElementId = "interactive-qr-reader";

  useEffect(() => {
    if (!isOpen) {
      stopCamera();
      setFeedback(null);
      setCameraError(null);
      return;
    }

    if (activeTab === 'camera') {
      startCamera();
    }

    return () => {
      stopCamera();
    };
  }, [isOpen, activeTab]);

  const startCamera = async () => {
    setCameraError(null);
    setFeedback(null);
    setIsScanning(true);

    try {
      if (!scannerRef.current) {
        scannerRef.current = new Html5Qrcode(readerElementId);
      }

      await scannerRef.current.start(
        { facingMode: "environment" },
        {
          fps: 15,
          qrbox: { width: 250, height: 250 },
          aspectRatio: 1.0,
        },
        handleDecodedText,
        () => {} // ignore frame errors
      );
    } catch (err) {
      console.warn("Camera start error:", err);
      setCameraError("Unable to access camera. Please check browser permissions or upload an image file.");
      setIsScanning(false);
    }
  };

  const stopCamera = async () => {
    if (scannerRef.current && scannerRef.current.isScanning) {
      try {
        await scannerRef.current.stop();
      } catch (err) {
        console.error("Camera stop error:", err);
      }
    }
  };

  const handleDecodedText = async (decodedText) => {
    let clean = decodedText.trim();
    // Normalize if contains LOC: or SKU:
    if (clean.toUpperCase().startsWith("LOC:")) clean = clean.substring(4).trim();
    if (clean.toUpperCase().startsWith("LOCATION:")) clean = clean.substring(9).trim();
    if (clean.includes(": ")) clean = clean.split(": ")[1].trim();

    if (targetCode) {
      const match = clean.toUpperCase() === targetCode.trim().toUpperCase();
      if (match) {
        setFeedback({ type: 'success', message: `Code verified successfully: ${clean}` });
        await stopCamera();
        setTimeout(() => {
          onScanSuccess(clean);
          onClose();
        }, 1200);
      } else {
        setFeedback({
          type: 'error',
          message: `Scanned code mismatch: "${clean}". Expected: "${targetCode}"`
        });
      }
    } else {
      setFeedback({ type: 'success', message: `Scanned code: ${clean}` });
      await stopCamera();
      setTimeout(() => {
        onScanSuccess(clean);
        onClose();
      }, 800);
    }
  };

  const handleFileUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setFeedback({ type: 'info', message: 'Analyzing image file...' });

    try {
      await stopCamera();
      const fileScanner = new Html5Qrcode(readerElementId);
      const result = await fileScanner.scanFile(file, true);
      handleDecodedText(result);
    } catch (err) {
      setFeedback({ type: 'error', message: 'No valid QR code detected in the selected image.' });
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title} icon={Camera} size="md">
      <div className="space-y-4">
        {/* Target indication if provided */}
        {targetCode && (
          <div className="p-3 bg-brand-500/10 border border-brand-500/20 rounded-xl text-center">
            <span className="text-xs uppercase font-bold text-slate-400 tracking-wider">Target Location:</span>
            <div className="text-xl font-mono font-bold text-brand-400 mt-0.5">{targetCode}</div>
          </div>
        )}

        {/* Tab switch */}
        <div className="flex bg-slate-900/80 p-1 rounded-xl border border-slate-800">
          <button
            onClick={() => setActiveTab('camera')}
            className={`flex-1 flex items-center justify-center gap-2 py-2 px-3 rounded-lg text-sm font-semibold transition-all ${
              activeTab === 'camera'
                ? 'bg-brand-600 text-white shadow-lg shadow-brand-600/30'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Camera className="w-4 h-4" /> Live Camera
          </button>
          <button
            onClick={() => {
              stopCamera();
              setActiveTab('file');
            }}
            className={`flex-1 flex items-center justify-center gap-2 py-2 px-3 rounded-lg text-sm font-semibold transition-all ${
              activeTab === 'file'
                ? 'bg-brand-600 text-white shadow-lg shadow-brand-600/30'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Upload className="w-4 h-4" /> Upload Image
          </button>
        </div>

        {/* Camera container */}
        <div className="relative rounded-2xl overflow-hidden bg-slate-950 border border-slate-800 min-h-[260px] flex items-center justify-center">
          <div id={readerElementId} className="w-full" />

          {activeTab === 'file' && (
            <div className="p-8 text-center w-full">
              <label className="flex flex-col items-center justify-center gap-3 p-6 border-2 border-dashed border-slate-700 hover:border-brand-500 rounded-2xl cursor-pointer bg-slate-900/50 hover:bg-slate-900 transition-all">
                <Upload className="w-10 h-10 text-brand-400 animate-bounce" />
                <div>
                  <span className="text-sm font-semibold text-white block">Click to select an image</span>
                  <span className="text-xs text-slate-400">Supports PNG, JPG, JPEG</span>
                </div>
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleFileUpload}
                  className="hidden"
                />
              </label>
            </div>
          )}

          {cameraError && activeTab === 'camera' && (
            <div className="p-6 text-center text-rose-400 space-y-2">
              <AlertCircle className="w-8 h-8 mx-auto text-rose-400" />
              <p className="text-sm font-medium">{cameraError}</p>
              <button
                onClick={startCamera}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-white transition-colors"
              >
                <RefreshCw className="w-3.5 h-3.5" /> Retry
              </button>
            </div>
          )}
        </div>

        {/* Status Feedback */}
        {feedback && (
          <div
            className={`p-3.5 rounded-xl border flex items-center gap-2.5 text-sm font-medium animate-fade-in ${
              feedback.type === 'success'
                ? 'bg-emerald-950/80 border-emerald-500/30 text-emerald-300'
                : feedback.type === 'error'
                ? 'bg-rose-950/80 border-rose-500/30 text-rose-300'
                : 'bg-brand-950/80 border-brand-500/30 text-brand-300'
            }`}
          >
            {feedback.type === 'success' ? (
              <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
            ) : (
              <AlertCircle className="w-5 h-5 text-rose-400 shrink-0" />
            )}
            <span>{feedback.message}</span>
          </div>
        )}
      </div>
    </Modal>
  );
}
