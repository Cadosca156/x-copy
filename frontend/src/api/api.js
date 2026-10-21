const API_URL = "/api";

export async function apiFetch(endpoint, options = {}) {
    const token = localStorage.getItem("token");

    const response = await fetch(`${API_URL}${endpoint}`, {
        ...options,
        headers: {
            ...(options.body && !(options.body instanceof FormData) && {
                "Content-Type": "application/json"
            }),

            ...(token && {
                Authorization: `Bearer ${token}`
            }),

            ...options.headers
        }
    });

    if (!response.ok) {
        let message = `Request failed: ${response.status}`;

        try {
            const errorData = await response.json();
            message = errorData.message || errorData.detail || message;
        } catch {
            // response не містить JSON
        }

        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}
