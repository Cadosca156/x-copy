import { useMemo, useState } from 'react';
import {
    Bell,
    Bookmark,
    ChevronDown,
    CircleEllipsis,
    Gift,
    Home,
    ImagePlus,
    ListPlus,
    MapPin,
    MessageCircle,
    MoreHorizontal,
    PenLine,
    Search,
    Share,
    Smile,
    Sparkles,
    UserRound,
    BarChart3,
    Heart,
    Repeat2,
} from 'lucide-react';
import '../styles/Feed.css';

const currentUser = {
    name: 'Alex Morgan',
    handle: '@alexmorgan',
    initials: 'AM',
    tone: 'violet',
};

const initialPosts = [
    {
        id: 1,
        name: 'Maya Chen',
        handle: '@mayacodes',
        time: '28m',
        initials: 'MC',
        tone: 'coral',
        text: 'Small reminder: the best interface is often the one that gets out of the way. Fewer decisions, clearer hierarchy, more room to think.',
        replies: 18,
        reposts: 42,
        likes: 386,
        views: '24K',
    },
    {
        id: 2,
        name: 'Northline Studio',
        handle: '@northline',
        time: '1h',
        initials: 'NS',
        tone: 'sky',
        text: 'A quiet corner of the city, right before the evening rush.',
        image: 'https://images.unsplash.com/photo-1519608487953-e999c86e7454?auto=format&fit=crop&w=1200&q=85',
        replies: 9,
        reposts: 31,
        likes: 214,
        views: '11K',
    },
    {
        id: 3,
        name: 'Jordan Reyes',
        handle: '@jordanreyes',
        time: '3h',
        initials: 'JR',
        tone: 'amber',
        text: 'What is one tool that made your workday meaningfully better this year? Looking for thoughtful recommendations — software, hardware, or a simple habit.',
        replies: 64,
        reposts: 16,
        likes: 129,
        views: '8,432',
    },
];

const trends = [
    { category: 'Technology · Trending', topic: 'Design systems', posts: '18.5K posts' },
    { category: 'Trending in Ukraine', topic: 'Kyiv', posts: '72.4K posts' },
    { category: 'Business & finance · Trending', topic: 'Creative economy', posts: '9,821 posts' },
    { category: 'Music · Trending', topic: 'Late night playlists', posts: '32.1K posts' },
];

const suggestedUsers = [
    { name: 'Nadia Petrenko', handle: '@nadia', initials: 'NP', tone: 'mint' },
    { name: 'Samir Patel', handle: '@samirbuilds', initials: 'SP', tone: 'rose' },
    { name: 'Olivia Carter', handle: '@oliviacarter', initials: 'OC', tone: 'blue' },
];

const navigation = [
    { label: 'Home', icon: Home, active: true },
    { label: 'Explore', icon: Search },
    { label: 'Notifications', icon: Bell },
    { label: 'Messages', icon: MessageCircle },
    { label: 'Bookmarks', icon: Bookmark },
    { label: 'Profile', icon: UserRound },
    { label: 'More', icon: CircleEllipsis },
];

function Avatar({ person, size = 'medium' }) {
    return <span className={`feed-avatar feed-avatar--${person.tone} feed-avatar--${size}`}>{person.initials}</span>;
}

function IconButton({ label, children, className = '', onClick }) {
    return (
        <button className={`icon-button ${className}`} type="button" aria-label={label} title={label} onClick={onClick}>
            {children}
        </button>
    );
}

function Post({ post }) {
    const [liked, setLiked] = useState(false);
    const toggleLike = () => {
        setLiked((value) => !value);
    };

    return (
        <article className="post">
            <Avatar person={post} />
            <div className="post__content">
                <div className="post__meta">
                    <span className="post__name">{post.name}</span>
                    <span className="post__handle">{post.handle}</span>
                    <span className="post__dot">·</span>
                    <time className="post__time">{post.time}</time>
                    <IconButton label="More post options" className="post__more"><MoreHorizontal size={18} /></IconButton>
                </div>
                <p className="post__text">{post.text}</p>
                {post.image && <img className="post__image" src={post.image} alt="City skyline at night" />}
                <div className="post__actions" aria-label="Post actions">
                    <button type="button" className="post-action post-action--reply"><MessageCircle size={18} /><span>{post.replies}</span></button>
                    <button type="button" className="post-action post-action--repost"><Repeat2 size={18} /><span>{post.reposts}</span></button>
                    <button type="button" onClick={toggleLike} className={`post-action post-action--like ${liked ? 'is-active' : ''}`}><Heart size={18} fill={liked ? 'currentColor' : 'none'} /><span>{post.likes + (liked ? 1 : 0)}</span></button>
                    <button type="button" className="post-action post-action--views"><BarChart3 size={18} /><span>{post.views}</span></button>
                    <button type="button" className="post-action post-action--share"><Share size={18} /></button>
                </div>
            </div>
        </article>
    );
}

export default function Feed() {
    const [activeTab, setActiveTab] = useState('For you');
    const [draft, setDraft] = useState('');
    const [posts, setPosts] = useState(initialPosts);
    const [following, setFollowing] = useState([]);
    const postCount = useMemo(() => draft.trim().length, [draft]);

    const submitPost = () => {
        if (!draft.trim()) return;
        setPosts((items) => [{
            id: Date.now(), ...currentUser, time: 'now', text: draft.trim(), replies: 0, reposts: 0, likes: 0, views: 0,
        }, ...items]);
        setDraft('');
    };

    return (
        <main className="feed-page">
            <aside className="feed-sidebar" aria-label="Main navigation">
                <div className="sidebar__top">
                    <a className="brand" href="#feed" aria-label="X home">X</a>
                    <nav className="sidebar__nav">
                        {navigation.map(({ label, icon: Icon, active }) => (
                            <button className={`nav-item ${active ? 'is-active' : ''}`} type="button" key={label}>
                                <Icon size={25} strokeWidth={active ? 2.6 : 2} />
                                <span>{label}</span>
                            </button>
                        ))}
                    </nav>
                    <button className="compose-button" type="button"><PenLine size={21} /><span>Post</span></button>
                </div>
                <button className="account-switcher" type="button">
                    <Avatar person={currentUser} size="small" />
                    <span className="account-switcher__text"><strong>{currentUser.name}</strong><small>{currentUser.handle}</small></span>
                    <MoreHorizontal size={19} />
                </button>
            </aside>

            <section className="timeline" id="feed" aria-label="Home feed">
                <header className="timeline__header">
                    <div className="timeline__title-row"><h1>Home</h1><IconButton label="Timeline settings"><Sparkles size={20} /></IconButton></div>
                    <div className="feed-tabs" role="tablist" aria-label="Feed type">
                        {['For you', 'Following'].map((tab) => <button key={tab} type="button" role="tab" aria-selected={activeTab === tab} onClick={() => setActiveTab(tab)} className={activeTab === tab ? 'is-active' : ''}>{tab}</button>)}
                    </div>
                </header>

                <section className="composer" aria-label="Create post">
                    <Avatar person={currentUser} />
                    <div className="composer__body">
                        <textarea value={draft} onChange={(event) => setDraft(event.target.value)} placeholder="What is happening?!" aria-label="Post text" maxLength={280} rows={2} />
                        <div className="composer__bottom">
                            <div className="composer__tools">
                                <IconButton label="Add image"><ImagePlus size={19} /></IconButton>
                                <IconButton label="Add GIF"><Gift size={19} /></IconButton>
                                <IconButton label="Add poll"><ListPlus size={19} /></IconButton>
                                <IconButton label="Add emoji"><Smile size={19} /></IconButton>
                                <IconButton label="Add location" className="composer__location"><MapPin size={19} /></IconButton>
                            </div>
                            <div className="composer__submit">
                                {postCount > 240 && <span className="character-count">{280 - postCount}</span>}
                                <button type="button" className="post-button" disabled={!draft.trim()} onClick={submitPost}>Post</button>
                            </div>
                        </div>
                    </div>
                </section>

                <div className="posts">
                    {posts.map((post) => <Post post={post} key={post.id} />)}
                </div>
            </section>

            <aside className="right-rail" aria-label="Discover">
                <label className="search-field"><Search size={19} /><input type="search" placeholder="Search" aria-label="Search" /></label>
                <section className="rail-card" aria-labelledby="happening-title">
                    <h2 id="happening-title">What’s happening</h2>
                    {trends.map((trend) => <button className="trend" type="button" key={trend.topic}><span>{trend.category}</span><strong>{trend.topic}</strong><small>{trend.posts}</small><MoreHorizontal size={18} /></button>)}
                    <button className="show-more" type="button">Show more</button>
                </section>
                <section className="rail-card" aria-labelledby="follow-title">
                    <h2 id="follow-title">Who to follow</h2>
                    {suggestedUsers.map((person) => {
                        const isFollowing = following.includes(person.handle);
                        return <div className="suggestion" key={person.handle}><Avatar person={person} size="small" /><div className="suggestion__text"><strong>{person.name}</strong><span>{person.handle}</span></div><button type="button" className={`follow-button ${isFollowing ? 'is-following' : ''}`} onClick={() => setFollowing((items) => isFollowing ? items.filter((handle) => handle !== person.handle) : [...items, person.handle])}>{isFollowing ? 'Following' : 'Follow'}</button></div>;
                    })}
                    <button className="show-more" type="button">Show more</button>
                </section>
                <footer className="rail-footer">Terms of Service&nbsp;&nbsp; Privacy Policy&nbsp;&nbsp; Cookie Policy&nbsp;&nbsp; Accessibility&nbsp;&nbsp; More <ChevronDown size={13} /> <span>© 2026</span></footer>
            </aside>

            <button className="mobile-compose" type="button" aria-label="Create a post"><PenLine size={22} /></button>
        </main>
    );
}
