import test from 'node:test';
import assert from 'node:assert/strict';
import {apiRequest, apiFetch} from './api.js';
test('mutations use cookie credentials and a fresh CSRF token without a bearer header',async t=>{
    const fetch=t.mock.method(globalThis,'fetch',async url=>url==='/api/auth/csrf'
        ? new Response(JSON.stringify({token:'masked-csrf'})) : new Response(null,{status:204}));
    await apiRequest('/users/me/logout',{method:'POST'});
    const [url,options]=fetch.mock.calls[1].arguments;
    assert.equal(url,'/api/users/me/logout');assert.equal(options.credentials,'same-origin');
    assert.equal(options.headers['X-XSRF-TOKEN'],'masked-csrf');assert.equal(options.headers.Authorization,undefined);
});
test('a failed CSRF check prevents the mutation',async t=>{
    const fetch=t.mock.method(globalThis,'fetch',async()=>new Response(null,{status:503}));
    await assert.rejects(apiRequest('/posts',{method:'DELETE'}));assert.equal(fetch.mock.callCount(),1);
});
test('reads require no additional CSRF request',async t=>{
    const fetch=t.mock.method(globalThis,'fetch',async()=>new Response('[]'));
    await apiRequest('/posts');assert.equal(fetch.mock.callCount(),1);
    assert.equal(fetch.mock.calls[0].arguments[1].credentials,'same-origin');
});

test('typed backend errors preserve the public message and status for the UI',async t=>{
    const fetch=t.mock.method(globalThis,'fetch');
    for (const [status,message] of [[400,'Додай текст, фото або відео'],[401,'Увійдіть в акаунт знову'],
        [403,'Можна видаляти лише власні коментарі'],[404,'Допис не знайдено'],
        [409,'Це ім’я користувача вже зайняте'],[500,'Не вдалося обробити запит'],
        [503,'Сервіс тимчасово зайнятий. Спробуйте пізніше.']]) {
        fetch.mock.mockImplementation(async()=>new Response(JSON.stringify({message,status,timestamp:Date.now()}),{status}));
        await assert.rejects(apiFetch('/posts'),error=>error.message===message&&error.status===status);
    }
});
