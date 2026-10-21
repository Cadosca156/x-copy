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
        let isCurrent = true;

        async function loadProfile() {
            try {
                const profile = await getMyProfile();

                if (isCurrent) {
                    setUsername(profile?.username ?? '');
                    setDisplayName(profile?.displayName ?? profile?.displayname ?? '');
                }
            } catch (loadError) {
                if (isCurrent) {
                    setError(loadError.message || 'Не вдалося завантажити профіль');
                }
            } finally {
                if (isCurrent) {
                    setIsLoading(false);
                }
            }
        }

        loadProfile();

        return () => {
            isCurrent = false;
        };
    }, []);

    const handleSubmit = async (event) => {
        event.preventDefault();
        const normalizedUsername = username.trim();
        const normalizedDisplayName = displayName.trim();

        if (!normalizedUsername || !normalizedDisplayName) {
            setError('Вкажіть username і display name');
            return;
        }

        setIsSaving(true);
        setError('');

        try {
            await updateMyProfile({
                username: normalizedUsername,
                displayName: normalizedDisplayName,
            });
            navigate('/feed', { replace: true });
        } catch (saveError) {
            setError(saveError.message || 'Не вдалося зберегти профіль');
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <main className="setup-profile-page">
            <section className="setup-profile-card" aria-labelledby="setup-profile-title">
                <span className="setup-profile-logo" aria-hidden="true">X</span>
                <h1 id="setup-profile-title">Налаштуйте профіль</h1>
                <p>Оберіть username і ім’я, яке бачитимуть інші користувачі.</p>

                <form onSubmit={handleSubmit}>
                    <label>
                        Username
                        <span className="setup-profile-username-prefix">@</span>
                        <input
                            type="text"
                            value={username}
                            onChange={(event) => setUsername(event.target.value)}
                            autoComplete="username"
                            maxLength={30}
                            disabled={isLoading || isSaving}
                            required
                        />
                    </label>
                    <label>
                        Display name
                        <input
                            type="text"
                            value={displayName}
                            onChange={(event) => setDisplayName(event.target.value)}
                            autoComplete="name"
                            maxLength={60}
                            disabled={isLoading || isSaving}
                            required
                        />
                    </label>

                    {error && <p className="setup-profile-error" role="alert">{error}</p>}

                    <button type="submit" disabled={isLoading || isSaving}>
                        {isLoading ? 'Завантажуємо…' : isSaving ? 'Зберігаємо…' : 'Зберегти та продовжити'}
                    </button>
                </form>
            </section>
        </main>
    );
}
