import { apiFetch } from './api.js';
import { emailInput } from './emailInput.js';

export function loginUser(login) {
    return apiFetch('/auth/login', { method: 'POST', body: JSON.stringify({ ...login, email: emailInput(login?.email) }) });
}
export function twoFactorVerify(challengeId, code) {
    return apiFetch('/auth/verify-2fa', { method: 'POST', body: JSON.stringify({ challengeId, code }) });
}
export function twoFactorResend(challengeId) {
    return apiFetch('/auth/send-code', { method: 'POST', body: JSON.stringify({ challengeId }) });
}
