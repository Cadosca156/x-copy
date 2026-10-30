import test from 'node:test';
import assert from 'node:assert/strict';
import { resetpassword } from './passwordResetApi.js';

test('missing reset token is rejected without making a request', async t => {
    const fetch = t.mock.method(globalThis, 'fetch', () => { throw new Error('Unexpected request'); });
    assert.equal((await resetpassword(null, 'new-password')).success, false);
    assert.equal((await resetpassword(' ', 'new-password')).success, false);
    assert.equal(fetch.mock.callCount(), 0);
});

test('sends the token from the reset link and the new password', async t => {
    const fetch = t.mock.method(globalThis, 'fetch', async url => url === '/api/auth/csrf' ? new Response(JSON.stringify({token:'csrf-token'})) : new Response(null, { status: 200 }));
    assert.equal((await resetpassword('reset-link-token', 'new-password')).success, true);
    const [url, options] = fetch.mock.calls[1].arguments;
    assert.equal(url, '/api/auth/password/reset');
    assert.equal(options.method, 'POST');
    assert.deepEqual(JSON.parse(options.body), { token: 'reset-link-token', newPassword: 'new-password' });
});

test('invalid or expired reset response is not reported as success', async t => {
    t.mock.method(globalThis, 'fetch', async url => url === '/api/auth/csrf' ? new Response(JSON.stringify({token:'csrf-token'})) : new Response(JSON.stringify({ message: 'Посилання недійсне' }), { status: 400 }));
    const result = await resetpassword('expired-token', 'new-password');
    assert.equal(result.success, false);
    assert.equal(result.message, 'Посилання недійсне');
});

test('network errors provide an actionable error message', async t => {
    t.mock.method(globalThis, 'fetch', async () => { throw new Error('Network unavailable'); });
    const result = await resetpassword('reset-token', 'new-password');
    assert.equal(result.success, false);
    assert.ok(result.message);
});
