import { apiFetch } from './api.js';

export function getMyProfile() {
    return apiFetch('/users/me');
}

export function getMyPasswordStatus() {
    return apiFetch('/users/me/password');
}

export function setMyInitialPassword(password) {
    return apiFetch('/users/me/password', {
        method: 'POST',
        body: JSON.stringify({ password }),
    });
}

export function updateMyProfile({ username, displayName }) {
    return apiFetch('/users/me/profile', {
        method: 'PATCH',
        body: JSON.stringify({ username, displayName }),
    });
}

export function updateMyAvatar(file) {
    const body = new FormData();
    body.append('file', file);
    return apiFetch('/users/me/avatar', { method: 'POST', body });
}
