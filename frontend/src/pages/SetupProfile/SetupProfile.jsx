import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyProfile, updateMyProfile } from '../../api/userApi.js';
import './SetupProfile.css';

export default function SetupProfile() {
    const navigate = useNavigate();
    const [username, setUsername] = useState('');
    const [displayName, setDisplayName] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [error, setError] = useState('');
    useEffect(() => {
        let current = true;
        getMyProfile().then(profile => {
            if (!current) return;
            setUsername(profile.username || '');
            setDisplayName(profile.displayName || '');
        }).catch(failure => {
            if (current) setError(failure.message || 'Не вдалося завантажити профіль');
        }).finally(() => { if (current) setIsLoading(false); });
        return () => { current = false; };
    }, []);

    async function submit(event) {
        event.preventDefault();
        if (isLoading || isSaving) return;
        const handle = username.trim().toLowerCase();
        const name = displayName.trim();
        if (!/^[a-z0-9_]{3,30}$/.test(handle)) {
            setError('Username: 3–30 латинських літер, цифр або знаків _');
            return;
        }
        if (!name || name.length > 50) {
            setError('Вкажи видиме ім’я довжиною від 1 до 50 символів');
            return;
        }
        setIsSaving(true);
        setError('');
        try {
            await updateMyProfile({ username: handle, displayName: name });
            navigate('/feed', { replace: true });
        } catch (failure) {
            setError(failure.message || 'Не вдалося зберегти профіль');
        } finally {
            setIsSaving(false);
        }
    }
    return (
        <main className="setup-profile-page">
            <section className="setup-profile-card" aria-labelledby="setup-profile-title">
                <span className="setup-profile-logo" aria-hidden="true">X</span>
                <h1 id="setup-profile-title">Налаштуй профіль</h1>
                <p>Обери username та ім’я, яке бачитимуть інші користувачі.</p>
                <form onSubmit={submit}>
                    <label>Username<input value={username} onChange={event => setUsername(event.target.value)} maxLength={30} autoComplete="username" disabled={isLoading || isSaving} required /></label>
                    <label>Видиме ім’я<input value={displayName} onChange={event => setDisplayName(event.target.value)} maxLength={50} autoComplete="name" disabled={isLoading || isSaving} required /></label>
                    {error && <p className="setup-profile-error" role="alert">{error}</p>}
                    <button type="submit" disabled={isLoading || isSaving}>{isLoading ? 'Завантажуємо…' : isSaving ? 'Зберігаємо…' : 'Зберегти та продовжити'}</button>
                </form>
            </section>
        </main>
    );
}
