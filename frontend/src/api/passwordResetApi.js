export async function sendemail(email) {
    try {
        const response = await fetch("/api/auth/password/forgot", {
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
    try {
        const response = await fetch("/api/auth/password/reset", {
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

        return { success: response.ok, data };
    }
    catch (error) {
        return { success: false, error };
    }
}
