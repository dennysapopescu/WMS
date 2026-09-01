import api from './client';

export const usersApi = {
  getAll: () =>
    api.get('/v1/admin/users').then((res) => res.data),

  create: (data) =>
    api.post('/v1/admin/users', data).then((res) => res.data),

  toggleStatus: (id) =>
    api.patch(`/v1/admin/users/${id}/toggle`).then((res) => res.data),
};
