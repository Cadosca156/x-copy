const API_URL = '/api';

export async function apiRequest(endpoint, options = {}) {
    const method = (options.method || 'GET').toUpperCase();
    const headers = { ...(options.body && !(options.body instanceof FormData) && { 'Content-Type': 'application/json' }), ...options.headers };
    if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
        const response = await fetch(`${API_URL}/auth/csrf`, { credentials: 'same-origin', cache: 'no-store' });
        if (!response.ok) throw new Error('Не вдалося підтвердити безпеку запиту. Оновіть сторінку.');
        const csrf = await response.json();
        if (!csrf.token) throw new Error('Оновіть сторінку та спробуйте знову.');
        headers['X-XSRF-TOKEN'] = csrf.token;
    }
    return fetch(`${API_URL}${endpoint}`, { ...options, credentials: 'same-origin', headers });
}

export async function apiFetch(endpoint, options = {}) {
    const response = await apiRequest(endpoint, options);
    if (!response.ok) {
        let message = response.status === 401 ? 'Сесію завершено. Увійдіть знову.' : `Request failed: ${response.status}`;
        try { const data = await response.json(); message = data.message || data.detail || message; } catch { /* Empty error body */ }
        const error = new Error(message);
        error.status = response.status;
        throw error;
    }
    if (response.status === 204) return null;
    return response.json();
}
