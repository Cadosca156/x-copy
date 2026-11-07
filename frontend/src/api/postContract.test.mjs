import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { MAX_POST_LENGTH, postContent, readPost } from './postContract.js';
import { readFeedPage } from './feedPagination.js';
import { createPost } from './postApi.js';

const post = JSON.parse(readFileSync(new URL('../../contracts/post-response.json', import.meta.url), 'utf8'));
test('shared backend fixture and nullable legacy author values follow the post contract', () => {
    assert.equal(readPost(post), post);
    assert.doesNotThrow(() => readPost({ ...post, userId: null, username: null, displayName: null, content: null, createdAt: null }));
    // Reading old posts does not enforce the new creation limit.
    assert.doesNotThrow(() => readPost({ ...post, content: 'x'.repeat(5000) }));
});
test('missing canonical fields, aliases and implicit type conversion are rejected', () => {
    const alias = { ...post, postId: post.id };delete alias.id;
    for (const invalid of [alias, { ...post, likedByMe: 'false' }, { ...post, likesCount: '2' },
        { ...post, commentsCount: -1 }, { ...post, mediaUrl: {} }, { ...post, createdAt: '2026' }]) {
        assert.throws(() => readPost(invalid));
        assert.throws(() => readFeedPage({ items: [invalid], nextCursor: null, hasMore: false }));
    }
});
test('creation uses 280 UTF-16 units, normalized whitespace and text-or-media', () => {
    assert.equal(MAX_POST_LENGTH, 280);
    assert.equal(postContent('  hello﻿ '), 'hello');
    assert.equal(postContent('😀'.repeat(140)).length, 280);
    assert.throws(() => postContent('😀'.repeat(141)));
    assert.throws(() => postContent('x'.repeat(281)));
    assert.throws(() => postContent('  ﻿ '));
    assert.throws(() => postContent('', new Blob([])));
    assert.equal(postContent(null, new Blob(['file'])), '');
});
test('invalid creation never reaches the network and successful creation validates the response', async t => {
    const fetch = t.mock.method(globalThis, 'fetch', async url => url === '/api/auth/csrf'
        ? new Response(JSON.stringify({ token: 'csrf' })) : new Response(JSON.stringify(post)));
    assert.throws(() => createPost('x'.repeat(281)));assert.throws(() => createPost(' '));
    assert.equal(fetch.mock.callCount(), 0);
    assert.deepEqual(await createPost('  Contract post  '), post);
    const form = fetch.mock.calls[1].arguments[1].body;assert.equal(form.get('content'), 'Contract post');
    fetch.mock.mockImplementation(async url => url === '/api/auth/csrf' ? new Response(JSON.stringify({ token: 'csrf' })) : new Response('{}'));
    await assert.rejects(createPost('valid'));
});
