import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Camera, Home, LogOut, UserRound, X } from 'lucide-react';
import Avatar from '../../components/Avatar.jsx';
import { getMyProfile, updateMyAvatar, updateMyProfile } from '../../api/userApi.js';
import { getUserPosts } from '../../api/postApi.js';
import '../Feed/Feed.css';
import './Profile.css';

const avatarTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];

export default function Profile() {
    const [profile, setProfile] = useState(null);
    const [displayName, setDisplayName] = useState('');
    const [selectedAvatar, setSelectedAvatar] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [error, setError] = useState('');
    const [notice, setNotice] = useState('');
    const [posts, setPosts] = useState([]);
    const [postsError, setPostsError] = useState('');
    const [isLoadingPosts, setIsLoadingPosts] = useState(true);
    const [retry, setRetry] = useState(0);

    function logout() {
        localStorage.removeItem('token');
        window.location.replace('/login');
    }

    useEffect(() => {
        let current = true;
        async function load() {
            try {
                const user = await getMyProfile();
                if (!current) return;
                setProfile(user);
                setDisplayName(user.displayName || '');
                setIsLoading(false);
                try {
                    const response = await getUserPosts(user.id);
                    if (current) setPosts(Array.isArray(response) ? response : response?.content ?? []);
                } catch (failure) {
                    if (current) setPostsError(failure.message || 'Не вдалося завантажити дописи');
                }
            } catch (failure) {
                if (current) setError(failure.message || 'Не вдалося завантажити профіль');
            } finally {
                if (current) { setIsLoading(false); setIsLoadingPosts(false); }
            }
        }
        load();
        return () => { current = false; };
    }, [retry]);

    useEffect(() => {
        if (!selectedAvatar) return;
        return () => URL.revokeObjectURL(selectedAvatar.url);
    }, [selectedAvatar]);

    function selectAvatar(event) {
        const file = event.target.files?.[0];
        event.target.value = '';
        if (!file || isSaving) return;
        setNotice('');
        if (!avatarTypes.includes(file.type)) { setError('Вибери фото JPEG, PNG, WebP або GIF'); return; }
        if (!file.size || file.size > 5 * 1024 * 1024) { setError('Вибери непорожнє фото розміром до 5 МБ'); return; }
        setError('');
        setSelectedAvatar({ file, url: URL.createObjectURL(file) });
    }

    async function saveName(event) {
        event.preventDefault();
        if (isSaving) return;
        const name = displayName.trim();
        if (!name || name.length > 50) { setError('Видиме ім’я має містити від 1 до 50 символів'); return; }
        setIsSaving(true);
        setError('');
        setNotice('');
        try {
            const saved = await updateMyProfile({ displayName: name });
            setProfile(saved);
            setDisplayName(saved.displayName);
            setNotice('Видиме ім’я збережено');
        } catch (failure) { setError(failure.message || 'Не вдалося зберегти ім’я'); }
        finally { setIsSaving(false); }
    }

    async function saveAvatar() {
        if (!selectedAvatar || isSaving) return;
        setIsSaving(true);
        setError('');
        setNotice('');
        try {
            const saved = await updateMyAvatar(selectedAvatar.file);
            setProfile(saved);
            setSelectedAvatar(null);
            setNotice('Аватар збережено');
        } catch (failure) { setError(failure.message || 'Не вдалося зберегти аватар'); }
        finally { setIsSaving(false); }
    }

    return (
        <main className="feed-page profile-page">
            <aside className="profile-navigation" aria-label="Навігація">
                <Link className="brand" to="/feed" aria-label="Головна">X</Link>
                <Link to="/feed"><Home size={24} /><span>Головна</span></Link>
                <Link to="/profile" className="is-active" aria-current="page"><UserRound size={24} /><span>Профіль</span></Link>
            </aside>
            <section className="timeline profile-timeline" aria-label="Мій профіль">
                <header className="profile-header">
                    <Link to="/feed" className="icon-button" aria-label="До стрічки"><ArrowLeft size={22} /></Link>
                    <h1>Мій профіль</h1>
                    <button type="button" className="profile-logout" onClick={logout}><LogOut size={18} aria-hidden="true" /><span>Вийти з акаунту</span></button>
                </header>
                {isLoading ? <p className="timeline-message">Завантажуємо профіль…</p> : !profile ? (
                    <div className="profile-load-error"><p role="alert">{error}</p><button type="button" onClick={() => { setIsLoading(true); setError(''); setRetry(value => value + 1); }}>Спробувати знову</button></div>
                ) : <>
                    <div className="profile-cover" aria-hidden="true" />
                    <div className="profile-details">
                        <Avatar person={profile} size="large" />
                        <h2>{profile.displayName || profile.username}</h2>
                        <p className="profile-handle">@{profile.username}</p>
                        <section className="profile-settings" aria-labelledby="profile-settings-title">
                            <h3 id="profile-settings-title">Редагування профілю</h3>
                            <div className="profile-avatar-editor">
                                <Avatar person={{ ...profile, avatarUrl: selectedAvatar?.url || profile.avatarUrl }} />
                                <label className="profile-photo-picker"><Camera size={18} />{selectedAvatar ? 'Вибрати інше фото' : 'Вибрати фото'}<input type="file" accept={avatarTypes.join(',')} onChange={selectAvatar} disabled={isSaving} aria-label="Вибрати аватар" /></label>
                            </div>
                            <p className="profile-help">JPEG, PNG, WebP або GIF · До 5 МБ</p>
                            {selectedAvatar && <div className="profile-photo-actions"><button type="button" className="profile-save" onClick={saveAvatar} disabled={isSaving}>{isSaving ? 'Зберігаємо…' : 'Зберегти фото'}</button><button type="button" className="profile-cancel" onClick={() => setSelectedAvatar(null)} disabled={isSaving}><X size={16} />Скасувати</button></div>}
                            <form onSubmit={saveName}>
                                <label className="profile-name-label" htmlFor="display-name">Видиме ім’я</label>
                                <input id="display-name" type="text" value={displayName} onChange={event => { setDisplayName(event.target.value); setNotice(''); }} maxLength={50} required disabled={isSaving} autoComplete="name" />
                                <p className="profile-help">Це ім’я бачитимуть інші користувачі. {displayName.length}/50</p>
                                <button className="profile-save" type="submit" disabled={isSaving || !displayName.trim() || displayName.trim() === profile.displayName}>{isSaving ? 'Зберігаємо…' : 'Зберегти ім’я'}</button>
                            </form>
                            {error && <p className="profile-error" role="alert">{error}</p>}
                            {notice && <p className="profile-success" role="status">{notice}</p>}
                        </section>
                    </div>
                    <h3 className="profile-posts-heading">Мої дописи</h3>
                    {isLoadingPosts && <p className="timeline-message">Завантажуємо дописи…</p>}
                    {postsError && <p className="timeline-message timeline-message--error" role="alert">{postsError}</p>}
                    {!isLoadingPosts && !postsError && posts.length === 0 && <p className="timeline-message">Твої дописи з’являться тут.<br /><Link to="/feed">Створити перший допис</Link></p>}
                    {posts.map(post => <article className="post" key={post.id}>
                        <Avatar person={profile} />
                        <div className="post__content">
                            <div className="profile-post-meta"><strong>{profile.displayName}</strong><span>@{profile.username}</span>{post.createdAt && <time dateTime={post.createdAt}>{new Date(post.createdAt).toLocaleDateString('uk-UA')}</time>}</div>
                            {post.content && <p className="post__text">{post.content}</p>}
                            {post.mediaUrl && (post.mediaType?.startsWith('video/') ? <video className="post__image" src={post.mediaUrl} controls playsInline preload="metadata" /> : <img className="post__image" src={post.mediaUrl} alt="Фото в дописі" loading="lazy" />)}
                        </div>
                    </article>)}
                </>}
            </section>
        </main>
    );
}
