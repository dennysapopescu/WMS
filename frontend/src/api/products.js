import api from './client';

export const productsApi = {
  getAll: () =>
    api.get('/v1/products/all').then((res) => res.data),

  search: (query, page = 0, size = 50) =>
    api.get('/v1/products', { params: { query, page, size } }).then((res) => res.data),

  getById: (id) =>
    api.get(`/v1/products/${id}`).then((res) => res.data),

  create: (data) =>
    api.post('/v1/products', data).then((res) => res.data),

  update: (id, data) =>
    api.put(`/v1/products/${id}`, data).then((res) => res.data),

  adjustStock: (id, data) =>
    api.patch(`/v1/products/${id}/stock`, data).then((res) => res.data),

  reduceQuantity: (id) =>
    api.post(`/v1/products/${id}/reduce`).then((res) => res.data),

  transfer: (productId, newLocationId) =>
    api.post('/v1/products/transfer', { productId, newLocationId }).then((res) => res.data),

  importCsv: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/v1/products/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((res) => res.data);
  },

  downloadTemplate: () => {
    window.location.href = '/api/v1/products/template';
  },

  exportPdf: () => {
    window.open('/api/v1/products/export-pdf', '_blank');
  },

  getQrUrl: (sku) => `/api/v1/products/qr/${encodeURIComponent(sku)}`,

  delete: (id) =>
    api.delete(`/v1/products/${id}`),
};
