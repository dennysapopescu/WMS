import api from './client';

export const dashboardApi = {
  getSummary: () =>
    api.get('/v1/dashboard').then((res) => res.data),
};
