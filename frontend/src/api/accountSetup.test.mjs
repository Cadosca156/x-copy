import test from 'node:test';
import assert from 'node:assert/strict';
import { getAccountSetupPath, validateInitialPassword } from './accountSetup.js';

test('Google accounts without a password are prompted, including existing profiles', () => {
    assert.equal(getAccountSetupPath({}, { passwordSet: false }), '/setup-password');
    assert.equal(getAccountSetupPath({ username: 'existing' }, { passwordSet: false }), '/setup-password');
});

test('accounts with a password continue to the profile setup or feed', () => {
    assert.equal(getAccountSetupPath({}, { passwordSet: true }), '/setup-profile');
    assert.equal(getAccountSetupPath({ username: 'existing' }, { passwordSet: true }), '/feed');
});

test('a missing status does not silently skip password creation', () => {
    assert.throws(() => getAccountSetupPath({}, {}));
});

test('password confirmation, minimum length and UTF-8 limit are enforced', () => {
    assert.equal(validateInitialPassword(' new-password ', ' new-password '), '');
    assert.notEqual(validateInitialPassword('short', 'short'), '');
    assert.notEqual(validateInitialPassword('        ', '        '), '');
    assert.notEqual(validateInitialPassword('new-password', 'other-password'), '');
    assert.notEqual(validateInitialPassword('я'.repeat(37), 'я'.repeat(37)), '');
    assert.equal(validateInitialPassword('a'.repeat(72), 'a'.repeat(72)), '');
});
