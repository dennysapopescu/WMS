import api from './client';

export const profileApi = {
  changePassword: (oldPassword, newPassword, confirmPassword) =>
    api.post('/v1/profile/change-password', { oldPassword, newPassword, confirmPassword }).then((res) => res.data),
};
