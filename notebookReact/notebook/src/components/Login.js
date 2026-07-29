import { useState } from "react";
import "../styles/login.css"
import {login} from "../utils/AuthBDStorage";
import {NavLink, useNavigate} from "react-router-dom";
export default function Login() {
    const navigate = useNavigate();
    const [invalidLogin, setInvalidLogin] = useState(false);
    const [loginUser, setLoginUser] = useState({
        email: "",
        password: "",
    });
    const handleLogin = async (e) => {
        e.preventDefault();
        await login(loginUser)

        const response = (await login(loginUser)).response;
    if(response.status === 200) {
        navigate("/notes")
        setInvalidLogin(false);
}
if(response.status === 404 || response.status === 400) {
    setInvalidLogin(true);
}

    }


    return (
        <div className="background-login">
            <div className="header-container">
                <form onSubmit={handleLogin} className="login-form">
                    <h2>Login</h2>
                    <div className={`invalid-container ${invalidLogin ? "active" : ""}`}>
                        <span>⚠</span>
                        <p>Invalid Email Or Password</p>
                    </div>
                    <input
                        type="text"
                        placeholder="Email"
                        className="email-input"
                        value={loginUser.email}
                        onChange={((e) =>
                            setLoginUser(prev => ({
                                ...prev,
                                email: e.target.value
                            })))}
                        required
                    />
                    <input
                        type="text"
                        placeholder="Password"
                        className="password-input"
                        value={loginUser.password}
                        onChange={((e) =>
                            setLoginUser(prev => ({
                                ...prev,
                                password: e.target.value
                            })))}
                        required
                    />
                    <button className={"login-button"} type="submit" >Login</button>
                </form>

                <NavLink to={"/"} className={"to-register"}>Don’t have an account? Register</NavLink>

            </div>
        </div>
    );
}