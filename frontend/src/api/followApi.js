import { apiFetch } from './api.js';

export const getSuggestedUsers = () => apiFetch('/users/suggestions');
export const getFollowingIds = () => apiFetch('/users/me/following');
export const getFollowingPosts = (page = 0) => apiFetch(`/users/me/following/posts?page=${page}&size=20`);
export const followUser = userId => apiFetch(`/users/${encodeURIComponent(userId)}/follow`, { method: 'PUT' });
export const unfollowUser = userId => apiFetch(`/users/${encodeURIComponent(userId)}/follow`, { method: 'DELETE' });
