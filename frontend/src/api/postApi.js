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

export function getPosts() {
    return apiFetch("/posts");
}

export function getUserPosts(userId) {
    return apiFetch(`/posts/user/${userId}`);
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
export function getComments(postId) {
    return apiFetch(`/posts/${postId}/comments`, {
        method: "GET"
    })
}
