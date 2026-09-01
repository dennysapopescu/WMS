import api from './client';

export const locationsApi = {
  getAll: () =>
    api.get('/v1/locations').then((res) => res.data),

  getById: (id) =>
    api.get(`/v1/locations/${id}`).then((res) => res.data),

  getProductsInLocation: (id) =>
    api.get(`/v1/locations/${id}/products`).then((res) => res.data),

  suggest: (quantity) =>
    api.get('/v1/locations/suggestions', { params: { quantity } }).then((res) => res.data),

  create: (data) =>
    api.post('/v1/locations', data).then((res) => res.data),

  update: (id, data) =>
    api.put(`/v1/locations/${id}`, data).then((res) => res.data),

  delete: (id) =>
    api.delete(`/v1/locations/${id}`),

  getQrUrl: (code) => `/api/v1/locations/qr/${encodeURIComponent(code)}`,
};
