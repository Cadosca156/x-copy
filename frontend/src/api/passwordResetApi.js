import { apiRequest } from './api.js';

export async function sendemail(email) {
    try {
        const response = await apiRequest('/auth/password/forgot', {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                email: email
            }),
        });

        const data = await response.json().catch(() => null);

        return { success: response.ok, data };
    }
    catch (error) {
        return { success: false, error };
    }
}
export async function resetpassword(token, newPassword) {
    if (!token || !token.trim()) {
        return { success: false, message: 'У посиланні немає токена. Запросіть новий лист.' };
    }
    try {
        const response = await apiRequest('/auth/password/reset', {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                token: token,
                newPassword: newPassword
            }),
        });

        const data = await response.json().catch(() => null);

        return {
            success: response.ok,
            data,
            message: response.ok ? undefined : data?.message || data?.detail || 'Не вдалося змінити пароль. Запросіть новий лист.',
        };
    }
    catch (error) {
        return { success: false, error, message: 'Не вдалося з’єднатися із сервером. Спробуйте ще раз.' };
    }
}
