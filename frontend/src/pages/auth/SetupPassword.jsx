import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getMyProfile, getMyPasswordStatus, setMyInitialPassword } from '../../api/userApi.js';
import { getAccountSetupPath, validateInitialPassword } from '../../api/accountSetup.js';
import '../SetupProfile/SetupProfile.css';

export default function SetupPassword() {
    const navigate = useNavigate();
    const [profile, setProfile] = useState(null);
    const [password, setPassword] = useState('');
    const [confirmation, setConfirmation] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        let current = true;
        if (!localStorage.getItem('token')) {
            navigate('/login', { replace: true });
            return;
        }
        Promise.all([getMyProfile(), getMyPasswordStatus()]).then(([account, status]) => {
            if (!current) return;
            const destination = getAccountSetupPath(account, status);
            if (destination !== '/setup-password') {
                navigate(destination, { replace: true });
                return;
            }
            setProfile(account);
        }).catch(failure => {
            if (current) setError(failure.message || 'Не вдалося завантажити акаунт');
        }).finally(() => {
            if (current) setIsLoading(false);
        });
        return () => { current = false; };
    }, [navigate]);

    async function submit(event) {
        event.preventDefault();
        if (!profile || isLoading || isSaving) return;
        const validationError = validateInitialPassword(password, confirmation);
        if (validationError) {
            setError(validationError);
            return;
        }
        setError('');
        setIsSaving(true);
        try {
            const status = await setMyInitialPassword(password);
            setPassword('');
            setConfirmation('');
            navigate(getAccountSetupPath(profile, status), { replace: true });
        } catch (failure) {
            setError(failure.message || 'Не вдалося зберегти пароль. Спробуй ще раз.');
        } finally {
            setIsSaving(false);
        }
    }

    const disabled = isLoading || isSaving || !profile;
    return (
        <main className="setup-profile-page">
            <section className="setup-profile-card" aria-labelledby="setup-password-title">
                <span className="setup-profile-logo" aria-hidden="true">X</span>
                <h1 id="setup-password-title">Створи пароль</h1>
                <p id="setup-password-help">Ти увійшов через Google. Задай пароль, щоб надалі входити також через email. Щонайменше 8 символів.</p>
                <form onSubmit={submit}>
                    <label>Новий пароль<input type="password" value={password} onChange={event => setPassword(event.target.value)} autoComplete="new-password" minLength={8} required disabled={disabled} aria-describedby="setup-password-help" /></label>
                    <label>Повтори пароль<input type="password" value={confirmation} onChange={event => setConfirmation(event.target.value)} autoComplete="new-password" minLength={8} required disabled={disabled} /></label>
                    {error && <p className="setup-profile-error" role="alert">{error}</p>}
                    <button type="submit" disabled={disabled}>{isLoading ? 'Завантажуємо…' : isSaving ? 'Зберігаємо…' : 'Зберегти та продовжити'}</button>
                    <Link to="/login">Повернутися до входу</Link>
                </form>
            </section>
        </main>
    );
}
