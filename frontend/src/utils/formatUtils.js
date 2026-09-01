/**
 * Shared utility functions to ensure 100% English formatting and normalization.
 */

export function formatLocationDescription(desc) {
  if (!desc) return 'General Sector';
  const d = desc.trim();
  if (/Electronice\s*High-Value/i.test(d)) return 'High-Value Electronics';
  if (/Electronice\s*-\s*Accesorii/i.test(d)) return 'Electronics - Accessories';
  if (/Produse\s*Voluminoase/i.test(d)) return 'Bulky & Oversized Goods';
  if (/Produse\s*Fragile/i.test(d) || /Sticl[aă]/i.test(d)) return 'Fragile Goods (Glassware)';
  if (/Zon[aă]\s*Retururi/i.test(d) || /Verificare/i.test(d)) return 'Returns & Inspection Zone';
  return d;
}

export function formatAction(action) {
  if (!action) return '-';
  const a = action.trim();
  if (/^AD[ĂA]UGARE$/i.test(a)) return 'ADD';
  if (/^MODIFICARE$/i.test(a)) return 'UPDATE';
  if (/^REDUCERE$/i.test(a)) return 'STOCK_REDUCTION';
  if (/^(ȘTERGERE|STERGERE)$/i.test(a)) return 'DELETE';
  if (/IMPORT\s*\([^\)]*REDIREC[ȚT]IONAT[^\)]*\)/i.test(a)) return 'IMPORT (REDIRECTED)';
  if (/^TRANSFER\s*(c[ăa]tre|to)\s*(.+)$/i.test(a)) {
    return a.replace(/^TRANSFER\s*(c[ăa]tre|to)\s*/i, 'TRANSFER to ');
  }
  if (/PICKING\s*FINALIZAT/i.test(a)) return 'PICKING_COMPLETED';
  return a;
}
