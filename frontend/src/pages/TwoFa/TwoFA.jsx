
import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ChevronLeft, X } from 'lucide-react';
import {twoFactorResend, twoFactorVerify} from "../../api/authApi.js";
import { getMyProfile } from '../../api/userApi.js';
import "./TwoFA.css";
import "./TwoFAResend.css";

const CODE_LENGTH = 6;
const RESEND_TIME = 5 * 60;

export default function TwoFA() {
    const [code, setCode] = useState(Array(CODE_LENGTH).fill(''));
    const inputs = useRef([]);

    const [resendTimer, setResendTimer] = useState(0);
    const [toast, setToast] = useState('');

    const completeCode = code.join('');

    const navigate = useNavigate();

    const challengeId = sessionStorage.getItem('loginChallenge');

    useEffect(() => {
        if (resendTimer <= 0) {
            return;
        }

        const timer = setInterval(() => {
            setResendTimer(prev => prev - 1);
        }, 1000);

        return () => clearInterval(timer);
    }, [resendTimer]);

    useEffect(() => {
        if (!toast) {
            return;
        }

        const timeout = setTimeout(() => {
            setToast('');
        }, 3000);

        return () => clearTimeout(timeout);
    }, [toast]);

    const handleTwoFactor = async (code) => {
        try {
            if (!challengeId) throw new Error('Почніть вхід знову');
            const data = await twoFactorVerify(challengeId, code);

            localStorage.setItem("token", data.token);
            sessionStorage.removeItem('loginChallenge');
            const profile = await getMyProfile();

            navigate(profile?.username ? '/feed' : '/setup-profile', { replace: true });
        } catch (error) {
            console.error(error);
            setToast(error.message || 'Не вдалося завершити вхід');
        }
    };

    const handleResendCode = async () => {
        if (resendTimer > 0) {
            setToast(
                `Please wait ${formatTime(resendTimer)} before requesting a new code`
            );
            return;
        }

        try {
            if (!challengeId) throw new Error('Почніть вхід знову');
            await twoFactorResend(challengeId);

            setResendTimer(RESEND_TIME);

            setToast("Verification code sent");
        } catch (error) {
            console.error(error);
            setToast("Failed to send verification code");
        }
    };

    const formatTime = (seconds) => {
        const minutes = Math.floor(seconds / 60);
        const remainingSeconds = seconds % 60;

        return `${minutes}:${remainingSeconds
    .toString()
    .padStart(2, '0')}`;
    };

    function updateCode(index, value) {
        const digits = value.replace(/\D/g, '');
        const next = [...code];

        if (digits.length > 1) {
            digits
                .slice(0, CODE_LENGTH - index)
                .split('')
                .forEach((digit, offset) => {
                    next[index + offset] = digit;
                });

            setCode(next);

            inputs.current[
                Math.min(index + digits.length, CODE_LENGTH - 1)
            ]?.focus();

            return;
        }

        next[index] = digits;
        setCode(next);

        if (digits && index < CODE_LENGTH - 1) {
            inputs.current[index + 1]?.focus();
        }
    }

    function handleKeyDown(index, event) {
        if (event.key === 'Backspace' && !code[index] && index > 0) {
            inputs.current[index - 1]?.focus();
        }
    }

    function handleSubmit(event) {
        event.preventDefault();

        if (completeCode.length === CODE_LENGTH) {
            handleTwoFactor(completeCode);
        }
    }

    return (
        <main className="two-factor-page">

            {toast && (
                <div className="two-factor-toast">
                    {toast}
                </div>
            )}

            <section
                className="two-factor-card"
                aria-labelledby="two-factor-title"
            >
                <header className="password-header">
                    <Link
                        to="/login"
                        className="back-button"
                        aria-label="Back to login"
                    >
                        <ChevronLeft size={23} />
                    </Link>

                    <X
                        className="login-logo"
                        size={27}
                        strokeWidth={2.5}
                        aria-label="X"
                    />
                </header>

                <form
                    onSubmit={handleSubmit}
                    className="two-factor-form"
                >
                    <h1 id="two-factor-title">
                        Enter confirmation code
                    </h1>

                    <p>
                        Enter the 6-digit code from your authenticator app.
                    </p>

                    <div
                        className="otp-inputs"
                        aria-label="Six digit confirmation code"
                    >
                        {code.map((digit, index) => (
                            <input
                                key={index}
                                ref={(element) => {
                                    inputs.current[index] = element;
                                }}
                                className="otp-input"
                                type="text"
                                inputMode="numeric"
                                autoComplete={
                                    index === 0
                                        ? 'one-time-code'
                                        : 'off'
                                }
                                maxLength={CODE_LENGTH}
                                value={digit}
                                onChange={(event) =>
                                    updateCode(
                                        index,
                                        event.target.value
                                    )
                                }
                                onKeyDown={(event) =>
                                    handleKeyDown(index, event)
                                }
                                aria-label={`Code digit ${index + 1}`}
                            />
                        ))}
                    </div>

                    <button
                        className="login-submit"
                        type="submit"
                        disabled={completeCode.length !== CODE_LENGTH}
                    >
                        Confirm
                    </button>

                    <div className="resend-section">
                        <button
                            type="button"
                            className="resend-button"
                            onClick={handleResendCode}
                        >
                            {resendTimer > 0
                                ? `Resend code in ${formatTime(resendTimer)}`
                                : "Resend code"
                            }
                        </button>
                    </div>

                    <Link
                        className="two-factor-help"
                        to="/login/help"
                    >
                        Having trouble?
                    </Link>
                </form>
            </section>
        </main>
    );
}
