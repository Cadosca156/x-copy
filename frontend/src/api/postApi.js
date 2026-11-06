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
export function toggleLike(postId) {

    return apiFetch(`/posts/${postId}/like`, {
        method: "POST"
    });

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
