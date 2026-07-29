import {useNavigate} from "react-router-dom";



export async function register(registerUser) {
    try {
        console.log("REGISTER USER:", registerUser);

        const response = await fetch(`/api/auth/register`, {

            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(
                registerUser,
            )
        });

        if (response.status === 409) {
            throw new Error("Email already exist");
        }
        if (response.status === 400)

        if (response.ok){
            console.log("REGISTER USER:", response.ok);
        }
        const data = await response.json();
        localStorage.setItem("token", data.token)

        return {data, response};
    }

    catch (error) {
        console.log(error);
    }

}
export async function login(loginUser) {
    try {


        const response = await fetch(`/api/auth/login`, {

            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(
                loginUser
            )
        });

        if (response.status === 404) {
            throw new Error("User not found");
        }

        if (response.ok){
            console.log("LOGIN USER:", response.ok)


        }
        const data = await response.json();
        localStorage.setItem("token", data.token)







        return {data, response};



    }
    catch (error) {
        console.log(error);

    }
}
