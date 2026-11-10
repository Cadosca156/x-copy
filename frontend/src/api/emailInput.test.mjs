import test from 'node:test';
import assert from 'node:assert/strict';
import {emailInput} from './emailInput.js';
import {loginUser} from './authApi.js';
import {sendemail} from './passwordResetApi.js';

test('email input trims edges without changing delivery spelling or aliases',()=>{
    assert.equal(emailInput(' \u00a0Owner.Name+Tag@Example.COM\uFEFF '),'Owner.Name+Tag@Example.COM');
    assert.equal(emailInput(null),null);
});

test('login trims email and preserves the exact password',async t=>{
    const fetch=t.mock.method(globalThis,'fetch',async url=>new Response(JSON.stringify(url.endsWith('/csrf')?{token:'csrf'}:{challengeId:'challenge'})));
    await loginUser({email:'  Owner.Name+Tag@Example.COM  ',password:' Unchanged Password '});
    assert.deepEqual(JSON.parse(fetch.mock.calls[1].arguments[1].body),{email:'Owner.Name+Tag@Example.COM',password:' Unchanged Password '});
});

test('password recovery uses the same input policy as login',async t=>{
    const fetch=t.mock.method(globalThis,'fetch',async url=>url.endsWith('/csrf')?new Response(JSON.stringify({token:'csrf'})):new Response(null,{status:202}));
    assert.equal((await sendemail('  Owner.Name+Tag@Example.COM  ')).success,true);
    assert.deepEqual(JSON.parse(fetch.mock.calls[1].arguments[1].body),{email:'Owner.Name+Tag@Example.COM'});
});
