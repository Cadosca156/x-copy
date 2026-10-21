
import { createRoot } from 'react-dom/client'

import { BrowserRouter, Routes, Route } from 'react-router-dom'

import './styles/index.css'
import Login from './pages/auth/Login.jsx'
import Feed from './pages/Feed/Feed.jsx';
import Profile from './pages/Profile/Profile.jsx';
import OAuth2Success from './pages/auth/OAuth2Success.jsx'
import TwoFA from "./pages/TwoFa/TwoFA.jsx";
import ResetPasswordEmail from "./pages/ResetPassword/ResetPasswordEmail.jsx";
import ResetPassword from "./pages/ResetPassword/ResetPassword.jsx";
import SetupProfile from './pages/SetupProfile/SetupProfile.jsx';

createRoot(document.getElementById('root')).render(
    <BrowserRouter>
        <Routes>
            <Route path="/" element={<Login/>}/>
            <Route path="/login" element={<Login/>}/>
            <Route path="/oauth2/success" element={<OAuth2Success/>}/>
            <Route path="/feed" element={<Feed />} />
            <Route path="/profile" element={<Profile />} />
            <Route path="/setup-profile" element={<SetupProfile />} />
            <Route path="/2fa" element={<TwoFA />} />
            <Route path="/reset-password-email" element={<ResetPasswordEmail />}/>
            <Route path="/reset-password" element={<ResetPassword />}/>
        </Routes>
    </BrowserRouter>
)
