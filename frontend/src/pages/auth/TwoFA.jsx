import { useRef, useState } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, X } from 'lucide-react';
import { twoFactorVerify } from "../../utils/AuthDBStorage.js";

const CODE_LENGTH = 6;

export default function TwoFA() {
    const [code, setCode] = useState(Array(CODE_LENGTH).fill(''));
    const inputs = useRef([]);

    const completeCode = code.join('');

    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    const userId = searchParams.get("userId");

    const handleTwoFactor = async (code) => {
        try {
            const data = await twoFactorVerify(userId, code);

            localStorage.setItem("token", data.token);

            navigate("/feed");

        } catch (error) {
            console.error(error);
        }
    };

    function updateCode(index, value) {
        const digits = value.replace(/\D/g, '');
        const next = [...code];

        if (digits.length > 1) {
            digits.slice(0, CODE_LENGTH - index)
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
            <section className="two-factor-card" aria-labelledby="two-factor-title">

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

                <form onSubmit={handleSubmit} className="two-factor-form">

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
                                    updateCode(index, event.target.value)
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