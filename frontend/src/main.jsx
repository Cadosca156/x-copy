
import { createRoot } from 'react-dom/client'

import { BrowserRouter, Routes, Route } from 'react-router-dom'

import './styles/index.css'
import Login from './pages/auth/Login.jsx'
import Feed from './pages/Feed.jsx';
import OAuth2Success from './pages/auth/OAuth2Success.jsx'
import TwoFA from "./pages/auth/TwoFA.jsx";

createRoot(document.getElementById('root')).render(
    <BrowserRouter>
        <Routes>
            <Route path="/" element={<Login/>}/>
            <Route path="/login" element={<Login/>}/>
            <Route path="/oauth2/success" element={<OAuth2Success/>}/>
            <Route path="/feed" element={<Feed />} />
            <Route path="/2fa" element={<TwoFA />} />

        </Routes>
    </BrowserRouter>
)
