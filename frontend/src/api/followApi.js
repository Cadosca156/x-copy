import { apiFetch } from './api.js';
import { feedPagePath, readFeedPage } from './feedPagination.js';

export const getSuggestedUsers = () => apiFetch('/users/suggestions');
export const getFollowingIds = () => apiFetch('/users/me/following');
export const getFollowingPosts = (cursor = null) => apiFetch(feedPagePath('/users/me/following/posts', cursor)).then(readFeedPage);
export const followUser = userId => apiFetch(`/users/${encodeURIComponent(userId)}/follow`, { method: 'PUT' });
export const unfollowUser = userId => apiFetch(`/users/${encodeURIComponent(userId)}/follow`, { method: 'DELETE' });
