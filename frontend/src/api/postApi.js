import { apiFetch } from "./api";

export function createPost(content, file) {
    const body = new FormData();
    body.append("content", content);
    if (file) body.append("file", file);

    return apiFetch("/posts", {
        method: "POST",
        body
    });
}

export function getPosts(page = 0) {
    return apiFetch(`/posts?page=${page}&size=20`);
}

export function getUserPosts(userId, page = 0) {
    return apiFetch(`/posts/user/${userId}?page=${page}&size=20`);
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
