import { apiFetch } from './api.js';

export function loginUser(login) {
    return apiFetch('/auth/login', { method: 'POST', body: JSON.stringify(login) });
}
export function twoFactorVerify(challengeId, code) {
    return apiFetch('/auth/verify-2fa', { method: 'POST', body: JSON.stringify({ challengeId, code }) });
}
export function twoFactorResend(challengeId) {
    return apiFetch('/auth/send-code', { method: 'POST', body: JSON.stringify({ challengeId }) });
}
