import api from './client';

export const authApi = {
  login: (username, password, rememberMe = false) =>
    api.post('/auth/login', { username, password, rememberMe }),

  logout: () =>
    api.post('/auth/logout'),

  getCurrentUser: () =>
    api.get('/auth/me'),
};
