import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Avatar from '../../components/Avatar.jsx';
import {
    Bell,
    Bookmark,
    ChevronDown,
    CircleEllipsis,
    Home,
    MessageCircle,
    MoreHorizontal,
    PenLine,
    Search,
    Share,
    UserRound,
    BarChart3,
    Heart,
    Repeat2,
    ImagePlus,
} from 'lucide-react';
import './Feed.css';
import './CreatePost.css'
import { createPost, deleteComment, deletePost, getComments, getPosts, postComment, toggleLike as togglePostLike } from "../../api/postApi.js";
import { getMyProfile } from '../../api/userApi.js';
import { followUser, unfollowUser, getFollowingIds, getFollowingPosts, getSuggestedUsers } from '../../api/followApi.js';

const currentUser = {
    displayName: 'Alex Morgan',
    username: 'alexmorgan',
    initials: 'AM',
    tone: 'violet',
};

const trends = [
    { category: 'Technology · Trending', topic: 'Design systems', posts: '18.5K posts' },
    { category: 'Trending in Ukraine', topic: 'Kyiv', posts: '72.4K posts' },
    { category: 'Business & finance · Trending', topic: 'Creative economy', posts: '9,821 posts' },
    { category: 'Music · Trending', topic: 'Late night playlists', posts: '32.1K posts' },
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



const avatarTones = ['violet', 'coral', 'sky', 'amber', 'mint', 'rose', 'blue'];
const likedPostsStorageKey = 'x-copy-liked-post-ids';
const acceptedMediaTypes = [
    'image/jpeg', 'image/png', 'image/gif', 'image/webp',
    'video/mp4', 'video/webm', 'video/quicktime',
];
// Leave room for multipart fields within the backend's 25 MB request limit.
const maxMediaSize = 24 * 1024 * 1024;

function getDisplayName(person) {
    return person?.displayName ?? person?.displayname ?? person?.name ?? person?.username ?? 'Unknown user';
}

function getUsername(person, fallbackName) {
    const username = person?.username ?? person?.handle ?? fallbackName;
    return String(username).replace(/^@/, '');
}

function getInitials(displayName) {
    return displayName.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'U';
}

function getStoredLikedPostIds() {
    try {
        const storedIds = JSON.parse(localStorage.getItem(likedPostsStorageKey) ?? '[]');
        return new Set(Array.isArray(storedIds) ? storedIds.map(String) : []);
    } catch {
        return new Set();
    }
}

function isPostStoredAsLiked(postId) {
    return getStoredLikedPostIds().has(String(postId));
}

function storePostLike(postId, isLiked) {
    try {
        const likedPostIds = getStoredLikedPostIds();

        if (isLiked) {
            likedPostIds.add(String(postId));
        } else {
            likedPostIds.delete(String(postId));
        }

        localStorage.setItem(likedPostsStorageKey, JSON.stringify([...likedPostIds]));
    } catch {
        // Серверний лайк уже збережений; локальний fallback просто недоступний.
    }
}

function formatPostTime(value) {
    if (!value) return '';

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);

    const diffInMinutes = Math.max(0, Math.floor((Date.now() - date.getTime()) / 60000));
    if (diffInMinutes < 1) return 'now';
    if (diffInMinutes < 60) return `${diffInMinutes}m`;
    if (diffInMinutes < 1440) return `${Math.floor(diffInMinutes / 60)}h`;
    if (diffInMinutes < 10080) return `${Math.floor(diffInMinutes / 1440)}d`;

    return date.toLocaleDateString();
}

function normalizePost(post, index = 0) {
    const author = post.author ?? post.user ?? post.createdBy ?? {};
    const displayName = getDisplayName({ ...author, displayName: author.displayName ?? post.authorDisplayName ?? post.displayName, name: author.name ?? post.authorName ?? post.name });
    const username = getUsername({ ...author, username: author.username ?? post.authorUsername ?? post.username, handle: author.handle ?? post.handle }, displayName);

    const id = post.id ?? post.postId ?? `${post.createdAt ?? 'post'}-${index}`;
    const serverLikedStatus = post.likedByMe ?? post.likedbyme ?? post.liked ?? post.isLiked ?? post.isLikedByCurrentUser ?? post.likedByCurrentUser ?? post.userLiked;

    return {
        id,
        userId: post.userId ?? author.id,
        displayName,
        avatarUrl: author.avatarUrl ?? post.avatarUrl,
        username,
        initials: getInitials(displayName),
        tone: avatarTones[index % avatarTones.length],
        time: formatPostTime(post.createdAt ?? post.created_on ?? post.date),
        text: post.content ?? post.text ?? '',
        mediaUrl: post.mediaUrl ?? post.imageUrl ?? post.image,
        mediaType: post.mediaType ?? 'image/jpeg',
        replies: Number(post.repliesCount ?? post.commentsCount ?? post.commentCount ?? 0),
        reposts: Number(post.repostsCount ?? 0),
        likes: Number(post.likesCount ?? post.likeCount ?? post.likes ?? 0),
        liked: Boolean(serverLikedStatus),
        hasLikedStatus: typeof serverLikedStatus === 'boolean',
        views: Number(post.viewsCount ?? 0),
    };
}

function normalizeComment(comment, index = 0) {
    const author = comment.author ?? comment.user ?? comment.createdBy ?? {};
    const displayName = getDisplayName({ ...author, displayName: author.displayName ?? comment.authorDisplayName ?? comment.displayName, name: author.name ?? comment.authorName ?? comment.name });
    const username = getUsername({ ...author, username: author.username ?? comment.authorUsername ?? comment.username, handle: author.handle ?? comment.handle }, displayName);

    return {
        id: comment.id ?? comment.commentId ?? `${comment.createdAt ?? 'comment'}-${index}`,
        displayName,
        username,
        text: comment.content ?? comment.text ?? comment.message ?? '',
        time: formatPostTime(comment.createdAt ?? comment.created_on ?? comment.date),
    };
}

function Post({ post, currentUserId, onDelete, isFollowing, followPending, onFollow }) {
    const isOwnPost = Boolean(currentUserId && post.userId && String(currentUserId) === String(post.userId));
    const [liked, setLiked] = useState(() => post.hasLikedStatus ? post.liked : isPostStoredAsLiked(post.id));
    const [likesCount, setLikesCount] = useState(post.likes);
    const [likeLoading, setLikeLoading] = useState(false);
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);
    const [deleteError, setDeleteError] = useState('');
    const [isCommentsOpen, setIsCommentsOpen] = useState(false);
    const [comments, setComments] = useState([]);
    const [commentsCount, setCommentsCount] = useState(post.replies);
    const [isLoadingComments, setIsLoadingComments] = useState(false);
    const [commentsError, setCommentsError] = useState('');
    const [commentText, setCommentText] = useState('');
    const [isPostingComment, setIsPostingComment] = useState(false);
    const [deletingCommentId, setDeletingCommentId] = useState(null);

    const [previousPost, setPreviousPost] = useState(post);

    // Sync refreshed server values before rendering, preserving local interaction state.
    if (previousPost.id !== post.id || previousPost.liked !== post.liked
        || previousPost.hasLikedStatus !== post.hasLikedStatus
        || previousPost.likes !== post.likes || previousPost.replies !== post.replies) {
        setPreviousPost(post);
        setLiked(post.hasLikedStatus ? post.liked : isPostStoredAsLiked(post.id));
        setLikesCount(post.likes);
        setCommentsCount(post.replies);
    }

    const loadComments = async () => {
        setIsLoadingComments(true);
        setCommentsError('');

        try {
            const response = await getComments(post.id);
            const items = Array.isArray(response) ? response : response?.content ?? response?.comments ?? [];
            const normalizedComments = items.map(normalizeComment);

            setComments(normalizedComments);
            setCommentsCount(Number(response?.totalElements ?? normalizedComments.length));
        } catch (error) {
            setCommentsError(error.message || 'Не вдалося завантажити коментарі');
        } finally {
            setIsLoadingComments(false);
        }
    };

    const handleCommentsToggle = () => {
        const nextIsOpen = !isCommentsOpen;
        setIsCommentsOpen(nextIsOpen);

        if (nextIsOpen) {
            loadComments();
        }
    };

    const handlePostComment = async (event) => {
        event.preventDefault();
        const content = commentText.trim();
        if (!content || isPostingComment) return;

        setIsPostingComment(true);
        setCommentsError('');

        try {
            await postComment(post.id, content);
            setCommentText('');
            await loadComments();
        } catch (error) {
            setCommentsError(error.message || 'Не вдалося додати коментар');
        } finally {
            setIsPostingComment(false);
        }
    };

    const handleDeleteComment = async (commentId) => {
        if (!window.confirm('Ви точно хочете видалити цей коментар?')) return;

        setDeletingCommentId(commentId);
        setCommentsError('');

        try {
            await deleteComment(post.id, commentId);
            setComments((items) => items.filter((comment) => comment.id !== commentId));
            setCommentsCount((count) => Math.max(0, count - 1));
        } catch (error) {
            setCommentsError(error.message || 'Не вдалося видалити коментар');
        } finally {
            setDeletingCommentId(null);
        }
    };

    const handleToggleLike = async () => {
        if (likeLoading) return;

        const previousLiked = liked;
        const previousLikesCount = likesCount;
        const nextLiked = !previousLiked;
        const nextLikesCount = Math.max(0, previousLikesCount + (nextLiked ? 1 : -1));

        setLiked(nextLiked);
        setLikesCount(nextLikesCount);
        setLikeLoading(true);

        try {
            const response = await togglePostLike(post.id);
            const serverLiked = response?.likedByMe ?? response?.likedbyme ?? response?.liked ?? response?.isLiked ?? response?.isLikedByCurrentUser ?? response?.likedByCurrentUser ?? response?.userLiked;
            const serverLikesCount = response?.likesCount ?? response?.likeCount ?? response?.likes;

            if (typeof serverLiked === 'boolean') {
                setLiked(serverLiked);
            }
            if (Number.isFinite(Number(serverLikesCount))) {
                setLikesCount(Number(serverLikesCount));
            }
            storePostLike(post.id, typeof serverLiked === 'boolean' ? serverLiked : nextLiked);
        } catch (error) {
            setLiked(previousLiked);
            setLikesCount(previousLikesCount);
            console.error('Unable to toggle post like:', error);
        } finally {
            setLikeLoading(false);
        }
    };

    const handleDelete = async () => {
        if (!isOwnPost || isDeleting) return;
        const isConfirmed = window.confirm('Ви точно хочете видалити цей пост? Цю дію не можна скасувати.');
        if (!isConfirmed) return;

        setIsDeleting(true);
        setDeleteError('');

        try {
            await deletePost(post.id);
            onDelete(post.id);
        } catch (error) {
            setDeleteError(error.message || 'Не вдалося видалити пост');
        } finally {
            setIsDeleting(false);
        }
    };

    return (
        <article className="post">
            <Avatar person={post} />
            <div className="post__content">
                <div className="post__meta">
                    <span className="post__name">{post.displayName}</span>
                    <span className="post__handle">@{post.username}</span>
                    <span className="post__dot">·</span>
                    <time className="post__time">{post.time}</time>
                    {currentUserId && post.userId && !isOwnPost && post.username !== 'deleted' && <button
                        type="button" className={`post-follow ${isFollowing ? 'is-following' : ''}`}
                        disabled={followPending} onClick={() => onFollow(post.userId)}
                        aria-label={`${isFollowing ? 'Відписатися від' : 'Підписатися на'} @${post.username}`}
                    >{followPending ? '…' : isFollowing ? 'Following' : 'Follow'}</button>}
                    {isOwnPost && <div className="post__menu">
                        <button
                            className="icon-button post__more"
                            type="button"
                            aria-label="More post options"
                            title="More post options"
                            aria-expanded={isMenuOpen}
                            onClick={() => setIsMenuOpen((isOpen) => !isOpen)}
                        >
                            <MoreHorizontal size={18} />
                        </button>
                        {isMenuOpen && (
                            <div className="post-menu" role="menu">
                                <button type="button" role="menuitem" onClick={handleDelete} disabled={isDeleting}>
                                    {isDeleting ? 'Видаляємо…' : 'Видалити'}
                                </button>
                            </div>
                        )}
                    </div>}
                </div>
                {post.text && <p className="post__text">{post.text}</p>}
                {post.mediaUrl && (
                    post.mediaType.startsWith('video/')
                        ? <video className="post__image post__video" src={post.mediaUrl} controls playsInline preload="metadata" />
                        : <img className="post__image" src={post.mediaUrl} alt={`Фото в дописі ${post.displayName}`} loading="lazy" />
                )}
                {deleteError && <p className="post__delete-error" role="alert">{deleteError}</p>}
                <div className="post__actions" aria-label="Post actions">
                    <button
                        type="button"
                        className={`post-action post-action--reply ${isCommentsOpen ? 'is-active' : ''}`}
                        onClick={handleCommentsToggle}
                        aria-expanded={isCommentsOpen}
                        aria-controls={`comments-${post.id}`}
                    >
                        <MessageCircle size={18} />
                        <span>{commentsCount}</span>
                    </button>
                    <button type="button" className="post-action post-action--repost"><Repeat2 size={18} /><span>{post.reposts}</span></button>
                    <button
                        type="button"
                        onClick={handleToggleLike}
                        disabled={likeLoading}
                        className={`post-action post-action--like ${liked ? 'is-active' : ''}`}
                        aria-label={liked ? 'Remove like' : 'Like post'}
                        aria-pressed={liked}
                    >
                        <Heart size={18} fill={liked ? 'currentColor' : 'none'} />
                        <span>{likesCount}</span>
                    </button>
                    <button type="button" className="post-action post-action--views"><BarChart3 size={18} /><span>{post.views}</span></button>
                    <button type="button" className="post-action post-action--share"><Share size={18} /></button>
                </div>
                {isCommentsOpen && (
                    <section className="comments" id={`comments-${post.id}`} aria-label="Comments">
                        {isLoadingComments && <p className="comments__message">Завантажуємо коментарі…</p>}
                        {commentsError && <p className="comments__message comments__message--error" role="alert">{commentsError}</p>}
                        {!isLoadingComments && !commentsError && comments.length === 0 && <p className="comments__message">Коментарів поки немає.</p>}
                        {comments.map((comment) => (
                            <div className="comment" key={comment.id}>
                                <div className="comment__content">
                                    <strong>{comment.displayName}</strong>
                                    <span>@{comment.username}</span>
                                    {comment.time && <time>· {comment.time}</time>}
                                    <p>{comment.text}</p>
                                </div>
                                <button
                                    className="comment__delete"
                                    type="button"
                                    onClick={() => handleDeleteComment(comment.id)}
                                    disabled={deletingCommentId === comment.id}
                                >
                                    {deletingCommentId === comment.id ? '…' : 'Видалити'}
                                </button>
                            </div>
                        ))}
                        <form className="comment-form" onSubmit={handlePostComment}>
                            <textarea
                                value={commentText}
                                onChange={(event) => setCommentText(event.target.value)}
                                placeholder="Напишіть коментар"
                                maxLength={280}
                                aria-label="New comment"
                            />
                            <button type="submit" disabled={!commentText.trim() || isPostingComment}>
                                {isPostingComment ? 'Надсилаємо…' : 'Коментувати'}
                            </button>
                        </form>
                    </section>
                )}
            </div>
        </article>
    );
}

export default function Feed() {
    const navigate = useNavigate();
    const timelineRef = useRef(null);
    const [activeTab, setActiveTab] = useState('For you');
    const [posts, setPosts] = useState([]);
    const [isLoadingPosts, setIsLoadingPosts] = useState(true);
    const [postsError, setPostsError] = useState('');
    const [following, setFollowing] = useState([]);
    const [suggestedUsers, setSuggestedUsers] = useState([]);
    const [isLoadingPeople, setIsLoadingPeople] = useState(true);
    const [followError, setFollowError] = useState('');
    const [followPending, setFollowPending] = useState([]);
    const [followingReady, setFollowingReady] = useState(false);
    const [feedVersion, setFeedVersion] = useState(0);
    const [currentProfile, setCurrentProfile] = useState(currentUser);

    const [isComposerOpen, setIsComposerOpen] = useState(false);
    const [postText, setPostText] = useState("");
    const [isPosting, setIsPosting] = useState(false);
    const [postError, setPostError] = useState("");



    const [selectedMedia, setSelectedMedia] = useState(null);

    useEffect(() => {
        if (!selectedMedia) return;
        return () => URL.revokeObjectURL(selectedMedia.url);
    }, [selectedMedia]);

    const emojis = ["😀", "😂", "❤️", "🔥", "😭", "😎", "👍", "💀"];

    useEffect(() => {
        let isCurrent = true;

        async function loadPosts() {
            setIsLoadingPosts(true);
            setPostsError('');

            try {
                const response = await (activeTab === 'Following' ? getFollowingPosts() : getPosts());
                const items = Array.isArray(response) ? response : response?.content ?? response?.posts ?? [];

                if (isCurrent) {
                    setPosts(items.map(normalizePost));
                }
            } catch (error) {
                if (isCurrent) {
                    setPostsError(error.message || 'Не вдалося завантажити пости');
                }
            } finally {
                if (isCurrent) {
                    setIsLoadingPosts(false);
                }
            }
        }

        loadPosts();
        return () => {
            isCurrent = false;
        };
    }, [activeTab, feedVersion]);

    useEffect(() => {
        let current = true;
        Promise.all([getSuggestedUsers(), getFollowingIds()])
            .then(([people, ids]) => {
                if (!current) return;
                setSuggestedUsers(people.slice(0, 3));
                setFollowing(ids.map(String));
                setFollowingReady(true);
            })
            .catch(error => { if (current) setFollowError(error.message || 'Не вдалося завантажити підписки'); })
            .finally(() => { if (current) setIsLoadingPeople(false); });
        return () => { current = false; };
    }, []);

    const handleFollow = async userId => {
        const id = String(userId);
        if (!followingReady || followPending.includes(id)) return;
        const wasFollowing = following.includes(id);
        setFollowPending(items => [...items, id]);
        setFollowError('');
        try {
            await (wasFollowing ? unfollowUser(id) : followUser(id));
            setFollowing(items => wasFollowing ? items.filter(item => item !== id) : [...new Set([...items, id])]);
            setFeedVersion(version => version + 1);
        } catch (error) {
            setFollowError(error.message || 'Не вдалося змінити підписку');
        } finally {
            setFollowPending(items => items.filter(item => item !== id));
        }
    };

    useEffect(() => {
        let isCurrent = true;

        getMyProfile()
            .then((profile) => {
                if (!isCurrent) return;

                const displayName = getDisplayName(profile);
                setCurrentProfile({
                    id: profile.id,
                    avatarUrl: profile.avatarUrl,
                    displayName,
                    username: getUsername(profile, displayName),
                    initials: getInitials(displayName),
                    tone: 'violet',
                });
            })
            .catch((error) => console.error('Unable to load current profile:', error));

        return () => {
            isCurrent = false;
        };
    }, []);

    const openComposer = () => {
        setPostText("");
        setPostError("");
        setSelectedMedia(null);
        setIsComposerOpen(true);
    };
    const closeComposer = () => {
        if (isPosting) return;

        setIsComposerOpen(false);
        setPostText("");
        setPostError("");
        setSelectedMedia(null);
    };

    const handleMediaChange = (event) => {
        const file = event.target.files?.[0];
        event.target.value = '';
        if (!file || isPosting) return;

        if (!acceptedMediaTypes.includes(file.type)) {
            setPostError('Вибери фото JPEG, PNG, GIF, WebP або відео MP4, WebM, MOV');
            return;
        }
        if (!file.size || file.size > maxMediaSize) {
            setPostError('Вибери непорожній файл розміром до 24 МБ');
            return;
        }

        setPostError('');
        setSelectedMedia({ file, url: URL.createObjectURL(file) });
    };

    const handleCreatePost = async () => {
        if (isPosting) return;
        const content = postText.trim();

        if (!content && !selectedMedia) {
            setPostError("Додай текст, фото або відео");
            return;
        }
        if (postText.length > 280) {
            setPostError('Текст має містити не більше 280 символів');
            return;
        }

        setIsPosting(true);
        setPostError("");

        try {
            await createPost(content, selectedMedia?.file);

            setPostText("");
            setSelectedMedia(null);
            setIsComposerOpen(false);
            setFeedVersion(version => version + 1);


        } catch (error) {
            console.error(error);
            setPostError(error.message || "Не вдалося створити пост");
        } finally {
            setIsPosting(false);
        }
    };



    return (
        <main className="feed-page feed-page--scrollable">
            <aside className="feed-sidebar" aria-label="Main navigation">
                <div className="sidebar__top">
                    <a className="brand" href="#feed" aria-label="X home">X</a>
                    <nav className="sidebar__nav">
                        {navigation.map(({ label, icon: Icon, active }) => (
                            <button className={`nav-item ${active ? 'is-active' : ''}`} type="button" key={label} onClick={label === 'Profile' ? () => navigate('/profile') : undefined}>
                                <Icon size={25} strokeWidth={active ? 2.6 : 2} />
                                <span>{label}</span>
                            </button>
                        ))}
                    </nav>
                    <button onClick={openComposer}className="compose-button" type="button"><PenLine size={21} /><span>Post</span></button>
                </div>
                <button className="account-switcher" type="button" onClick={() => navigate('/profile')}>
                    <Avatar person={currentProfile} size="small" />
                    <span className="account-switcher__text"><strong>{currentProfile.displayName}</strong><small>@{currentProfile.username}</small></span>
                    <MoreHorizontal size={19} />
                </button>
            </aside>

            <section className="timeline" aria-label="Posts" ref={timelineRef}>
                <header className="timeline__header">
                    <div className="timeline__title-row">
                        <h1>Home</h1>
                        <button className="icon-button" type="button" onClick={() => navigate('/profile')} aria-label="Мій профіль"><Avatar person={currentProfile} size="small" /></button>
                    </div>
                    <div className="feed-tabs" role="tablist" aria-label="Feed type">
                        {['For you', 'Following'].map((tab) => (
                            <button
                                className={activeTab === tab ? 'is-active' : ''}
                                type="button"
                                role="tab"
                                aria-selected={activeTab === tab}
                                key={tab}
                                onClick={() => { setActiveTab(tab); timelineRef.current?.scrollTo({ top: 0 }); }}
                            >
                                {tab}
                            </button>
                        ))}
                    </div>
                </header>

                {isLoadingPosts && <p className="timeline-message">Завантажуємо пости…</p>}
                {postsError && <p className="timeline-message timeline-message--error">{postsError}</p>}
                {followError && <p className="timeline-message timeline-message--error" role="alert">{followError}</p>}
                {!isLoadingPosts && !postsError && posts.length === 0 && <p className="timeline-message">{activeTab === 'Following' ? 'Тут будуть дописи користувачів, на яких ти підписаний. Підпишись на автора у стрічці або в списку праворуч.' : 'Постів поки немає.'}</p>}
                {!isLoadingPosts && !postsError && posts.map((post) => (
                    <Post
                        post={post}
                        currentUserId={currentProfile.id}
                        isFollowing={following.includes(String(post.userId))}
                        followPending={!followingReady || followPending.includes(String(post.userId))}
                        onFollow={handleFollow}
                        key={post.id}
                        onDelete={(postId) => setPosts((items) => items.filter((item) => item.id !== postId))}
                    />
                ))}
            </section>

            {isComposerOpen && (
                <div className="composer-overlay" onMouseDown={closeComposer}>
                    <div
                        className="composer-modal"
                        onMouseDown={(e) => e.stopPropagation()}
                    >
                        <div className="composer-header">
                            <button
                                className="composer-close"
                                onClick={closeComposer}
                                disabled={isPosting}
                                aria-label="Закрити форму допису"
                            >
                                ×
                            </button>
                        </div>

                        <div className="composer-body">

                            <Avatar person={currentProfile} size="small" />

                            <div className="composer-content">

                    <textarea
                        autoFocus
                        value={postText}
                        onChange={(e) => setPostText(e.target.value)}
                        placeholder="What is happening?!"
                        maxLength={280}
                        disabled={isPosting}
                    />

                                {selectedMedia && (
                                    <div className="image-preview">
                                        {selectedMedia.file.type.startsWith('video/') ? (
                                            <video src={selectedMedia.url} controls playsInline preload="metadata" />
                                        ) : (
                                            <img src={selectedMedia.url} alt="Попередній перегляд фото" />
                                        )}

                                        <button
                                            className="remove-image"
                                            type="button"
                                            aria-label="Видалити прикріплений файл"
                                            disabled={isPosting}
                                            onClick={() => {
                                                setSelectedMedia(null);
                                                setPostError('');
                                            }}
                                        >
                                            ×
                                        </button>
                                    </div>
                                )}

                                {postError && (
                                    <div className="composer-error" role="alert">
                                        {postError}
                                    </div>
                                )}

                                <div className="composer-divider"></div>

                                <div className="composer-bottom">

                                    <div className="composer-tools">

                                        <label className="tool-button media-picker" title="Додати фото або відео">
                                            <ImagePlus size={21} aria-hidden="true" />
                                            <input
                                                type="file"
                                                accept={acceptedMediaTypes.join(',')}
                                                aria-label="Додати фото або відео"
                                                disabled={isPosting}
                                                onChange={handleMediaChange}
                                            />
                                        </label>

                                        <button
                                            className="tool-button"
                                            type="button"
                                        >
                                            GIF
                                        </button>

                                        <button
                                            className="tool-button"
                                            type="button"
                                        >
                                            📊
                                        </button>

                                        <button
                                            className="tool-button"
                                            type="button"
                                            disabled={isPosting}
                                            onClick={() => {
                                                setPostText(
                                                    postText + " " + emojis[0]
                                                );
                                            }}
                                        >
                                            😊
                                        </button>

                                    </div>

                                    <div className="composer-actions">

                            <span
                                className={`character-count ${
                                    postText.length >= 260
                                        ? "character-warning"
                                        : ""
                                }`}
                            >
                                {postText.length}/280
                            </span>

                                        <button
                                            className="publish-button"
                                            onClick={handleCreatePost}
                                            disabled={
                                                (!postText.trim() && !selectedMedia) ||
                                                postText.length > 280 ||
                                                isPosting
                                            }
                                        >
                                            {isPosting ? "Posting..." : "Post"}
                                        </button>

                                    </div>
                                </div>
                                <p className="media-hint">Фото JPEG, PNG, GIF, WebP · Відео MP4, WebM, MOV · До 24 МБ</p>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            <aside className="right-rail" aria-label="Discover">
                <label className="search-field"><Search size={19} /><input type="search" placeholder="Search" aria-label="Search" /></label>
                <section className="rail-card" aria-labelledby="happening-title">
                    <h2 id="happening-title">What’s happening</h2>
                    {trends.map((trend) => <button className="trend" type="button" key={trend.topic}><span>{trend.category}</span><strong>{trend.topic}</strong><small>{trend.posts}</small><MoreHorizontal size={18} /></button>)}
                    <button className="show-more" type="button">Show more</button>
                </section>
                <section className="rail-card" aria-labelledby="follow-title">
                    <h2 id="follow-title">Who to follow</h2>
                    {isLoadingPeople && <p className="people-message">Завантажуємо користувачів…</p>}
                    {!isLoadingPeople && !followError && suggestedUsers.length === 0 && <p className="people-message">Інших користувачів поки немає.</p>}
                    {suggestedUsers.map((person) => {
                        const isFollowing = following.includes(String(person.id));
                        const pending = !followingReady || followPending.includes(String(person.id));
                        return <div className="suggestion" key={person.id}><Avatar person={person} size="small" /><div className="suggestion__text"><strong>{getDisplayName(person)}</strong><span>@{person.username}</span></div><button type="button" className={`follow-button ${isFollowing ? 'is-following' : ''}`} disabled={pending} onClick={() => handleFollow(person.id)} aria-label={`${isFollowing ? 'Відписатися від' : 'Підписатися на'} @${person.username}`}>{pending ? '…' : isFollowing ? 'Following' : 'Follow'}</button></div>;
                    })}
                </section>
                <footer className="rail-footer">Terms of Service&nbsp;&nbsp; Privacy Policy&nbsp;&nbsp; Cookie Policy&nbsp;&nbsp; Accessibility&nbsp;&nbsp; More <ChevronDown size={13} /> <span>© 2026</span></footer>
            </aside>

            <button className="mobile-compose" type="button" onClick={openComposer} aria-label="Create a post"><PenLine size={22} /></button>
        </main>
    );
}
