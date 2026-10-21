import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getMyProfile } from '../../api/userApi.js';

function OAuth2Success() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [error, setError] = useState('');

    useEffect(() => {
        const token = searchParams.get("token");

        if (!token) {
            navigate("/login");
            return;
        }

        async function redirectAfterProfileCheck() {
            try {
                localStorage.setItem("token", token);
                const profile = await getMyProfile();
                navigate(profile?.username ? '/feed' : '/setup-profile', { replace: true });
            } catch (profileError) {
                setError(profileError.message || 'Не вдалося завантажити профіль');
            }
        }

        redirectAfterProfileCheck();
    }, [navigate, searchParams]);


    return <div>{error || 'Logging in...'}</div>;
}

export default OAuth2Success;
