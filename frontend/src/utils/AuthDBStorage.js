import {data, useNavigate} from "react-router-dom";

export async function loginUser(login) {
    try {


        const response = await fetch(`/api/auth/login`, {

            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(
                login
            )
        });

        if (response.status === 404) {
            throw new Error("User not found");
        }

        if (response.ok) {
            console.log("LOGIN USER:", response.ok)


        }


        console.log("STATUS:", response.status);
        console.log("URL:", response.url);

        const text = await response;
        console.log("RESPONSE:", text);

        return {response};


    } catch (error) {
        console.log(error);

    }
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

        if (response.ok) {
            const data = await response.json();

            console.log("JWT:", data.token);
        }
        return data;
    }
    catch (error) {
        console.log(error);
    }
}