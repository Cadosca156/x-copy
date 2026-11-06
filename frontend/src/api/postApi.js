import { apiFetch } from "./api.js";
import { feedPagePath, readFeedPage } from './feedPagination.js';

export function createPost(content, file) {
    const body = new FormData();
    body.append("content", content);
    if (file) body.append("file", file);

    return apiFetch("/posts", {
        method: "POST",
        body
    });
}

export function getPosts(cursor = null) {
    return apiFetch(feedPagePath('/posts', cursor)).then(readFeedPage);
}

export function getUserPosts(userId, cursor = null) {
    return apiFetch(feedPagePath(`/posts/user/${encodeURIComponent(userId)}`, cursor)).then(readFeedPage);
}

export function deletePost(id) {
    return apiFetch(`/posts/${id}`, {
        method: "DELETE"
    });
}
export async function setLike(postId, liked) {
    if (typeof liked !== 'boolean') throw new Error('Вкажіть бажаний стан лайка');
    const state = await apiFetch(`/posts/${encodeURIComponent(postId)}/like`, {
        method: liked ? 'PUT' : 'DELETE'
    });
    if (typeof state?.likedByMe !== 'boolean' || !Number.isSafeInteger(state.likesCount) || state.likesCount < 0) {
        throw new Error('Не вдалося підтвердити стан лайка. Оновіть стрічку.');
    }
    return state;
}
export function postComment(id, content) {
    return apiFetch(`/posts/${id}/comments`, {
        method: "POST",
        body: JSON.stringify({ content })
    });
}
export function deleteComment(id,commentId) {
    return apiFetch(`/posts/${id}/comments/${commentId}`, {
        method: "DELETE"
    })
}
export function getComments(postId, page = 0) {
    return apiFetch(`/posts/${postId}/comments?page=${page}&size=20`, {
        method: "GET"
    })
}
