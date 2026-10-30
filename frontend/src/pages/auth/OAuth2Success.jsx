import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { apiFetch } from '../../api/api.js';
import { getMyProfile, getMyPasswordStatus } from '../../api/userApi.js';
import { getAccountSetupPath } from '../../api/accountSetup.js';

export default function OAuth2Success() {
    const navigate = useNavigate();
    const [params] = useSearchParams();
    const [error, setError] = useState('');
    const started = useRef(false);
    useEffect(() => {
        if (started.current) return;
        started.current = true;
        const code = params.get('code');
        window.history.replaceState({}, '', window.location.pathname);
        if (!code) { navigate('/login', { replace: true }); return; }
        async function finish() {
            try {
                await apiFetch('/auth/oauth/exchange', { method: 'POST', body: JSON.stringify({ code }) });
                const [profile, status] = await Promise.all([getMyProfile(), getMyPasswordStatus()]);
                navigate(getAccountSetupPath(profile, status), { replace: true });
            } catch (failure) { setError(failure.message || 'Спробуйте увійти знову'); }
        }
        finish();
    }, [navigate, params]);
    return <main>{error ? <><p role="alert">{error}</p><Link to="/login">Повернутися до входу</Link></> : 'Входимо…'}</main>;
}
