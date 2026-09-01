import api from './client';

export const ordersApi = {
  getAll: () =>
    api.get('/v1/outbound-orders').then((res) => res.data),

  getHistory: () =>
    api.get('/v1/outbound-orders/history').then((res) => res.data),

  getById: (id) =>
    api.get(`/v1/outbound-orders/${id}`).then((res) => res.data),

  create: (sku, requestedQuantity) =>
    api.post('/v1/outbound-orders', { sku, requestedQuantity }).then((res) => res.data),

  confirmPick: (id, scannedLocationCode) =>
    api.post(`/v1/outbound-orders/${id}/confirm-pick`, { scannedLocationCode }).then((res) => res.data),

  scanConfirm: (orderId, scannedCode) =>
    api.post('/v1/outbound-orders/scan-confirm', null, {
      params: { orderId, scannedCode }
    }).then((res) => res.data),

  cancel: (id) =>
    api.delete(`/v1/outbound-orders/${id}`),
};
