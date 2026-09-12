
import '../styles/navbarStyles/navbar.css';
import {useState} from "react";
import {NavLink} from "react-router-dom";


export default function Navbar() {
    const [animateNavBtn, setAnimateNavBtn] = useState(false);

    return (
        <div className="navbar-container">
            <div className="navbar-button-container">
            <button className= {`navbar-button ${animateNavBtn ? "active" : ""}`} onClick={ ()=>setAnimateNavBtn(!animateNavBtn) }>
                <svg viewBox="0 0 100 80" width="40" height="40" fill="grey">
                    <rect width="100" height="15" rx="8"></rect>
                    <rect y="30" width="100" height="15" rx="8"></rect>
                    <rect y="60" width="100" height="15" rx="8"></rect>
                </svg>
            </button>
        <div
               className={`navbar ${animateNavBtn ? "active" : ""}`}>

            <h1
                className={`welcome `}>SkyNotes</h1>
                <div className="link-container">


                    <NavLink to="/notes" className={({ isActive }) =>
                        isActive ? "nav-link active" : "nav-link"
                    }><button className={"link-button"}>
                        Notes
                    </button> </NavLink>

                <NavLink to="/weather"  className={({ isActive }) =>
                    isActive ? "nav-link active" : "nav-link"
                }>
                    <button className={"link-button"} >
                    Weather
                </button>



                </NavLink>

                </div>

            <button className={`logout-button `} onClick={() => {
                localStorage.removeItem("token");
                window.location.href = "/login";
            }}>
                <span className={`logout-text `}>Logout</span>
            </button>

        </div>
        </div>
        </div>
    )
}