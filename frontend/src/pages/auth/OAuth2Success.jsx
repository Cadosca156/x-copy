import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

function OAuth2Success() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    useEffect(() => {
        const token = searchParams.get("token");

        if (token) {
            localStorage.setItem("token", token);
            navigate("/feed");
        } else {
            navigate("/login");
        }
    }, [navigate, searchParams]);

    return <div>Logging in...</div>;
}

export default OAuth2Success;