import test from 'node:test';
import assert from 'node:assert/strict';
import { feedPagePath, readFeedPage } from './feedPagination.js';
import { readFileSync } from 'node:fs';
const post = JSON.parse(readFileSync(new URL('../../contracts/post-response.json', import.meta.url), 'utf8'));

test('feed pagination sends opaque positions without offset and preserves URL encoding', () => {
    const first = new URL(feedPagePath('/posts'), 'https://example.test');
    assert.equal(first.searchParams.get('pagination'), 'cursor');
    assert.equal(first.searchParams.get('size'), '20');assert.equal(first.searchParams.has('page'), false);
    assert.equal(first.searchParams.has('cursor'), false);
    const next = new URL(feedPagePath('/posts', 'abc_-/+=&'), 'https://example.test');
    assert.equal(next.searchParams.get('cursor'), 'abc_-/+=&');
});
test('server lookahead decides the end instead of page length', () => {
    const fullLast = { items: Array.from({ length: 20 }, (_, id) => ({ ...post, id: String(id) })), hasMore: false, nextCursor: null };
    assert.equal(readFeedPage(fullLast).hasMore, false);
    const next = { items: [post], hasMore: true, nextCursor: 'opaque' };
    assert.equal(readFeedPage(next).nextCursor, 'opaque');
    for (const invalid of [[], { items: [], hasMore: true, nextCursor: 'x' }, { items: [], hasMore: true, nextCursor: null }]) {
        assert.throws(() => readFeedPage(invalid));
    }
});
