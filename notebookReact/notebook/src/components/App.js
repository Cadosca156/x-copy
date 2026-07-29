import {Routes, Route, useLocation} from "react-router-dom";
import Notes from "./Notes";
import WeatherMap from "./WeatherMap";
import "../styles/background.css"
import Register from "./Register";
import Login from "./Login";
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