export async function loginUser(login) {
    const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(login),
    });
    if (!response.ok) {
        const text = await response.text();
        let message = `Не вдалося увійти (HTTP ${response.status})`;
        try {
            const error = JSON.parse(text);
            message = error.message || error.detail || message;
        } catch {
            if (text.trim() === 'Invalid CORS request') {
                message = 'Сервер не дозволяє вхід із цього сайту. Потрібно оновити налаштування CORS бекенду.';
            }
        }
        throw new Error(message);
    }
    return { response };
}
export async function twoFactorVerify(userId,code) {
    try {
        const response = await fetch("/api/auth/verify-2fa", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                userId,
                code,
            }),
        });


            const data = await response.json();

            console.log("JWT:", data.token);

        console.log("2FA DATA:", data);
        console.log("TOKEN:", data?.token);

        if (!data?.token) {
            console.error("Backend не повернув JWT");
            return;
        }

        return data;
    }
    catch (error) {
        console.log(error);
    }
}
export async function twoFactorResend(userId) {
    try {
        await fetch("/api/auth/send-code", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                userId
            }),
        });


    }
    catch (error) {
        console.log(error);
    }
}
