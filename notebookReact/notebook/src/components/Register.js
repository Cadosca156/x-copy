import { useState } from "react";
import "../styles/register.css"
import { register} from "../utils/AuthBDStorage";
import {NavLink,useNavigate} from "react-router-dom";

export default function Register() {
    const [invalidRegister, setInvalidRegister] = useState(false);
    const navigate = useNavigate();
    const [registerUser, setRegisterUser] = useState({
        username: "",
        email: "",
        password: "",
    });

    const handleRegister = async (e) => {
        e.preventDefault();

        const { response } = await register(registerUser);

       switch (response.status) {
           case 200:
               navigate("/notes");
               setInvalidRegister(false);
            break;
           case 403:
           case 400:
                   setInvalidRegister(true);
                   break;
           default:
               console.log("Unexpected status:", response.status);

       }
    }


    return (
        <div className="background-register">
            <div className="header-container">
        <form onSubmit={handleRegister} className="register-form">
            <h2>Register</h2>
            <div className={`invalid-container ${invalidRegister ? "active" : ""}`}>
                <span>⚠</span>
                <p>Your Username must be minimum 3 chars and password minimum 8 chars</p>
            </div>
            <input
                type="text"
                placeholder="Username"
                className={"username-input"}
                value={registerUser.username}
                onChange={((e) =>
                    setRegisterUser(prev => ({
                        ...prev,
                        username: e.target.value
                    })))}
                required
            />
            <input
                type="text"
                placeholder="Email"
                value={registerUser.email}
                className={"email-input"}
                onChange={((e) =>
                    setRegisterUser(prev => ({
                        ...prev,
                        email: e.target.value
                    })))}
                required
            />
            <input
                type="text"
                placeholder="Password"
                value={registerUser.password}
                className={"password-input"}
                onChange={((e) =>
                    setRegisterUser(prev => ({
                        ...prev,
                        password: e.target.value
                    })))}
                required
            />
            <button className={"register-button"} type="submit" >Register</button>
        </form>
                <NavLink to={"/login"} className={"to-login"}>Already registered? Login</NavLink>
            </div>
        </div>
    );
}