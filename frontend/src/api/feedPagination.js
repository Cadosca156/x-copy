export function feedPagePath(path, cursor = null) {
    const params = new URLSearchParams({ pagination: 'cursor', size: '20' });
    if (cursor !== null) params.set('cursor', cursor);
    return `${path}?${params}`;
}

export function readFeedPage(response) {
    if (!response || !Array.isArray(response.items) || typeof response.hasMore !== 'boolean'
        || !(response.nextCursor === null || typeof response.nextCursor === 'string')
        || (response.hasMore && (!response.nextCursor || response.items.length === 0))
        || (!response.hasMore && response.nextCursor !== null)) {
        throw new Error('Не вдалося завантажити стрічку. Спробуйте пізніше.');
    }
    return response;
}
