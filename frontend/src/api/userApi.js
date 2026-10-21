import { apiFetch } from './api.js';

export function getMyProfile() {
    return apiFetch('/users/me');
}

export function updateMyAvatar(file) {
    const body = new FormData();
    body.append('file', file);
    return apiFetch('/users/me/avatar', { method: 'POST', body });
}

export function updateMyProfile({ username, displayName }) {
    return apiFetch('/users/me/profile', {
        method: 'PATCH',
        body: JSON.stringify({ username, displayName }),
    });
}
