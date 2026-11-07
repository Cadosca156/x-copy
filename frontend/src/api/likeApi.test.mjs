import test from 'node:test';
import assert from 'node:assert/strict';
import { setLike } from './postApi.js';

test('repeated likes and unlikes send the desired state, never POST toggle', async t => {
    const fetch = t.mock.method(globalThis, 'fetch', async (url, options) => url === '/api/auth/csrf'
        ? new Response(JSON.stringify({ token: 'masked' }))
        : new Response(JSON.stringify({ likedByMe: options.method === 'PUT', likesCount: options.method === 'PUT' ? 1 : 0 })));
    for (const desired of [true, true, false, false]) assert.equal((await setLike('post', desired)).likedByMe, desired);
    const mutations = fetch.mock.calls.filter(call => call.arguments[0] !== '/api/auth/csrf');
    assert.deepEqual(mutations.map(call => call.arguments[1].method), ['PUT', 'PUT', 'DELETE', 'DELETE']);
    assert.ok(mutations.every(call => call.arguments[1].credentials === 'same-origin'));
});
test('ambiguous payloads and failed requests are not treated as confirmed state', async t => {
    const fetch = t.mock.method(globalThis, 'fetch', async url => url === '/api/auth/csrf'
        ? new Response(JSON.stringify({ token: 'masked' })) : new Response(JSON.stringify({ liked: true })));
    await assert.rejects(setLike('post', true), /підтвердити/);
    fetch.mock.mockImplementation(async url => url === '/api/auth/csrf' ? new Response(JSON.stringify({ token: 'masked' })) : new Response(null, { status: 503 }));
    await assert.rejects(setLike('post', false));
    await assert.rejects(setLike('post', 'toggle'), /бажаний/);
});
