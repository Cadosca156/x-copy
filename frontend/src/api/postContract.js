export const MAX_POST_LENGTH = 280;

export function postContent(content, file) {
    const text = content ?? '';
    if (typeof text !== 'string' || text.length > MAX_POST_LENGTH) {
        throw new Error(`Текст допису має містити до ${MAX_POST_LENGTH} символів`);
    }
    const normalized = text.trim();
    if (!normalized && !(file && file.size > 0)) throw new Error('Додай текст, фото або відео');
    return normalized;
}

// PostResponse is flat. Nullable legacy values are allowed; aliases and type coercion are not.
export function readPost(post) {
    const nullableStrings = ['userId', 'username', 'displayName', 'content', 'mediaType', 'mediaUrl', 'createdAt', 'avatarUrl'];
    if (!post || typeof post !== 'object' || typeof post.id !== 'string' || !post.id
        || nullableStrings.some(key => !Object.hasOwn(post, key) || !(post[key] === null || typeof post[key] === 'string'))
        || !Number.isSafeInteger(post.likesCount) || post.likesCount < 0
        || !Number.isSafeInteger(post.commentsCount) || post.commentsCount < 0
        || typeof post.likedByMe !== 'boolean'
        || (post.createdAt !== null && (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?$/.test(post.createdAt)
            || !Number.isFinite(Date.parse(post.createdAt))))) {
        throw new Error('Не вдалося завантажити допис. Оновіть сторінку.');
    }
    return post;
}
