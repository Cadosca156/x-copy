import {Routes, Route, useLocation} from "react-router-dom";
import Notes from "./pages/Notes";
import WeatherMap from "./pages/WeatherMap";
import "../styles/noteStyles/background.css"
import Register from "./pages/Register";
import Login from "./pages/Login";
import ProtectedRoute from "./ProtectedRoute";
import Navbar from "./Navbar";
import {useState} from "react";

export default function App() {


    const location = useLocation();

    const hideNavbar =
        location.pathname === "/" ||
        location.pathname === "/login";

    return (
        <div>

            <Routes>
           <Route path={"/"} element={<Register/>}/>
            <Route path={"/login"} element={<Login/>}/>

            <Route path={"/notes"} element={
                <ProtectedRoute>
                <Notes/>
                </ProtectedRoute>
                }
            />
            <Route path={"/weather"} element={
                <ProtectedRoute>
                    <WeatherMap/>
                </ProtectedRoute>
            }></Route>




            </Routes>
        </div>
    );
}