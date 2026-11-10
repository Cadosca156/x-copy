import {test,expect} from '@playwright/test';
import {beginLogin,identity,login,mutate,password,register,waitForMail} from './helpers.mjs';

test.describe.configure({mode:'serial'});
const pageErrors = new WeakMap();
test.beforeEach(async({page})=>{
    const errors = [];pageErrors.set(page,errors);
    page.on('pageerror',error=>errors.push(error.message));
    page.on('response',response=>{
        if(new URL(response.url()).pathname.startsWith('/api/') && response.status()>=500)
            errors.push('Unexpected HTTP '+response.status()+' '+new URL(response.url()).pathname);
    });
});
test.afterEach(async({page})=>expect(pageErrors.get(page)).toEqual([]));

test('email signup, real mail code, profile, media post, comment, like and logout',async({page,context})=>{
    const identityData = identity();
    await register(page,{...identityData,email:identityData.email.toUpperCase()});
    const originalAccount = await (await context.request.get('/api/users/me')).json();
    const cookies = await context.cookies();const session = cookies.find(cookie=>cookie.name==='XCOPY_SESSION_ID');
    expect(session?.httpOnly).toBe(true);expect(session.sameSite).toBe('Lax');
    expect(session.path).toBe('/');expect(session.value).toMatch(/^[0-9a-f-]{36}$/i);
    expect(await page.evaluate(()=>localStorage.getItem('token'))).toBeNull();
    expect(await page.evaluate(()=>document.cookie)).not.toContain('XCOPY_SESSION');
    const content = 'Real E2E post '+Date.now();
    await page.getByRole('button',{name:'Post',exact:true}).click();
    await page.getByPlaceholder('What is happening?!').fill(content);
    await page.getByLabel('Додати фото або відео').setInputFiles({
        name:'pixel.png',mimeType:'image/png',
        buffer:Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jE1sAAAAASUVORK5CYII=','base64')
    });
    const creation = page.waitForResponse(response=>new URL(response.url()).pathname==='/api/posts'&&response.request().method()==='POST');
    await page.locator('.composer-modal').getByRole('button',{name:'Post',exact:true}).click();
    const created = await (await creation).json();
    const post = page.locator('article.post').filter({has:page.getByText(content,{exact:true})});
    await expect(post).toBeVisible();
    const image = post.locator('img.post__image');await expect(image).toBeVisible();
    await expect.poll(()=>image.evaluate(image=>image.complete&&image.naturalWidth>0)).toBe(true);
    // Execute the real PUT, then lose only its response to the browser.
    await page.route('**/api/posts/'+created.id+'/like',async route=>{
        const response = await route.fetch();expect(response.status()).toBe(200);
        await route.abort('failed');
    },{times:1});
    await post.getByRole('button',{name:'Like post',exact:true}).click();
    await expect(post.getByRole('button',{name:'Like post',exact:true})).toBeEnabled();
    await expect(post.getByRole('alert')).toContainText('Не вдалося підтвердити стан лайка');
    const committed = await (await context.request.get('/api/posts?pagination=cursor')).json();
    expect(committed.items.find(item=>item.id===created.id)).toMatchObject({likedByMe:true,likesCount:1});
    await post.getByRole('button',{name:'Like post',exact:true}).click();
    await expect(post.getByRole('button',{name:'Remove like',exact:true})).toBeVisible();
    await expect(post.getByRole('alert')).toHaveCount(0);
    for(let i=0;i<2;i++){
        const liked = await mutate(context.request,'/api/posts/'+created.id+'/like',{method:'PUT'});
        expect(liked.status()).toBe(200);expect(await liked.json()).toEqual({likedByMe:true,likesCount:1});
    }
    for(let i=0;i<2;i++){
        const unliked = await mutate(context.request,'/api/posts/'+created.id+'/like',{method:'DELETE'});
        expect(unliked.status()).toBe(200);expect(await unliked.json()).toEqual({likedByMe:false,likesCount:0});
    }
    await page.reload();await expect(post.getByRole('button',{name:'Like post',exact:true})).toBeVisible();
    await post.getByRole('button',{name:'Like post',exact:true}).click();
    await expect(post.getByRole('button',{name:'Remove like',exact:true})).toBeVisible();
    await post.locator('.post-action--reply').click();
    await post.getByLabel('New comment').fill('E2E comment');
    await post.locator('.comment-form button[type=submit]').click();
    await expect(post.getByText('E2E comment',{exact:true})).toBeVisible();
    page.once('dialog',dialog=>dialog.accept());
    await post.locator('.comment').getByRole('button',{name:'Видалити',exact:true}).click();
    await expect(post.getByText('E2E comment',{exact:true})).toHaveCount(0);
    await post.getByLabel('New comment').fill('Cascade comment');
    await post.locator('.comment-form button[type=submit]').click();
    await expect(post.getByText('Cascade comment',{exact:true})).toBeVisible();
    await post.getByRole('button',{name:'More post options'}).click();
    page.once('dialog',dialog=>dialog.accept());
    await post.getByRole('menuitem',{name:'Видалити',exact:true}).click();await expect(post).toHaveCount(0);
    expect((await mutate(context.request,'/api/posts/'+created.id,{method:'DELETE'})).status()).toBe(204);
    expect((await context.request.get('/api/posts/'+created.id+'/comments')).status()).toBe(404);
    expect((await mutate(context.request,'/api/posts/'+created.id+'/like',{method:'PUT'})).status()).toBe(404);
    const removedLike = await mutate(context.request,'/api/posts/'+created.id+'/like',{method:'DELETE'});
    expect(await removedLike.json()).toEqual({likedByMe:false,likesCount:0});
    await expect.poll(async()=>(await context.request.get(created.mediaUrl)).status(),{timeout:15000}).toBe(404);
    await page.goto('/profile');
    await page.getByRole('button',{name:'Вийти з акаунту'}).click();await expect(page).toHaveURL(/\/login$/);
    const replay = await context.request.get('/api/users/me',{headers:{Cookie:'XCOPY_SESSION_ID='+session.value}});
    expect(replay.status()).toBe(401);
    await login(page,identityData.email.toLowerCase());
    await expect(page).toHaveURL(/\/feed$/);
    expect((await (await context.request.get('/api/users/me')).json()).id).toBe(originalAccount.id);
});

test('password reset uses real mail, revokes both devices, rejects reuse and permits the new password',async({page,context,browser})=>{
    const user = await register(page);
    const oldSession = (await context.cookies()).find(cookie=>cookie.name==='XCOPY_SESSION_ID').value;
    const other = await browser.newContext();
    try {
    const otherPage = await other.newPage();await login(otherPage,user.email);
    await expect(otherPage).toHaveURL(/\/feed$/);
    await page.goto('/reset-password-email');
    await page.getByLabel('Email',{exact:true}).fill(user.email.toUpperCase());
    const forgot = page.waitForResponse(response=>response.url().endsWith('/api/auth/password/forgot')&&response.request().method()==='POST');
    await page.getByRole('button',{name:'Продовжити',exact:true}).click();
    expect((await forgot).status()).toBe(202);
    const unknown = await mutate(context.request,'/api/auth/password/forgot',{data:{email:identity().email}});
    expect(unknown.status()).toBe(202);expect(await unknown.text()).toBe('');
    const mail = await waitForMail(user.email,'Reset password');
    const link = mail.Text.match(/http:\/\/localhost:15173\/reset-password\?token=[^\s]+/)[0];
    const token = new URL(link).searchParams.get('token');
    await page.goto(link);
    const newPassword = 'E2e-reset-password-456';
    await page.getByLabel('Новий пароль',{exact:true}).fill(newPassword);
    await page.getByLabel('Повторіть пароль',{exact:true}).fill(newPassword);
    await page.getByRole('button',{name:'Змінити пароль',exact:true}).click();
    await expect(page.getByRole('button',{name:'Перейти до входу'})).toBeVisible();
    const revoked = await context.request.get('/api/users/me',{headers:{Cookie:'XCOPY_SESSION_ID='+oldSession}});
    expect(revoked.status()).toBe(401);
    expect((await other.request.get('/api/users/me')).status()).toBe(401);
    const reuse = await mutate(context.request,'/api/auth/password/reset',{data:{token,newPassword:'Another-password-789'}});
    expect(reuse.status()).toBe(400);
    await page.goto('/login');
    await page.locator('input[type=email]').fill(user.email);await page.locator('input[type=password]').fill(password);
    await page.getByRole('button',{name:'Продолжить',exact:true}).click();
    await expect(page.getByRole('alert')).toContainText(/парол|email|емейл/i);
    await login(page,user.email,newPassword);await expect(page).toHaveURL(/\/feed$/);
    } finally {await other.close();}
});

test('reauthentication rotates the session and logout leaves another device signed in',async({page,context,browser})=>{
    const user = await register(page);
    const first = (await context.cookies()).find(cookie=>cookie.name==='XCOPY_SESSION_ID').value;
    const other = await browser.newContext();
    try {
        const secondPage = await other.newPage();await login(secondPage,user.email);
        await login(page,user.email);
        const rotated = (await context.cookies()).find(cookie=>cookie.name==='XCOPY_SESSION_ID').value;
        expect(rotated).not.toBe(first);
        // A standalone request cannot replace the current browser's cookie when replaying the old ID.
        const stale = await fetch('http://localhost:15173/api/users/me',{headers:{Cookie:'XCOPY_SESSION_ID='+first}});
        expect(stale.status).toBe(401);
        expect((await mutate(context.request,'/api/users/me/logout')).status()).toBe(204);
        expect((await context.request.get('/api/users/me')).status()).toBe(401);
        expect((await other.request.get('/api/users/me')).status()).toBe(200);
    } finally {await other.close();}
});

test('real database pagination, CSRF rejection, spoofed upload and post ownership',async({page,context,browser})=>{
    await register(page);
    expect((await context.request.post('/api/posts',{multipart:{content:'No CSRF'}})).status()).toBe(403);
    // Validate the raw payload on the server, even when a client bypasses the composer.
    for (const content of ['x'.repeat(281), 'x' + ' '.repeat(280), '\u00a0\uFEFF']) {
        expect((await mutate(context.request,'/api/posts',{multipart:{content}})).status()).toBe(400);
    }
    const spoofed = await mutate(context.request,'/api/posts',{multipart:{
        content:'Spoofed upload',file:{name:'photo.png',mimeType:'image/png',buffer:Buffer.from('<script>spoof</script>')}
    }});expect(spoofed.status()).toBe(415);
    const ids = [];
    for(let i=0;i<21;i++){
        const response = await mutate(context.request,'/api/posts',{multipart:{content:'Pagination E2E '+i}});
        expect(response.status()).toBe(201);ids.push((await response.json()).id);
    }
    await page.reload();await expect(page.locator('article.post')).toHaveCount(20);
    const firstWindow = await (await context.request.get('/api/posts?pagination=cursor&size=20')).json();
    expect(firstWindow.items).toHaveLength(20);expect(firstWindow.hasMore).toBe(true);expect(firstWindow.nextCursor).toBeTruthy();
    const late = await mutate(context.request,'/api/posts',{multipart:{content:'New post during cursor scrolling'}});
    expect(late.status()).toBe(201);
    const anchor = firstWindow.items.at(-1).id;
    expect((await mutate(context.request,'/api/posts/'+anchor,{method:'DELETE'})).status()).toBe(204);
    await page.getByRole('button',{name:'Завантажити ще',exact:true}).click();
    await expect(page.locator('article.post')).toHaveCount(21);
    await expect(page.getByText('New post during cursor scrolling',{exact:true})).toHaveCount(0);
    await expect(page.getByRole('button',{name:'Завантажити ще',exact:true})).toHaveCount(0);
    expect((await context.request.get('/api/posts?pagination=cursor&cursor=bad')).status()).toBe(400);
    expect((await context.request.get('/api/posts?pagination=cursor&page=0')).status()).toBe(400);
    expect((await context.request.get('/api/posts?page=0&size=100000')).status()).toBe(400);
    const owner = await (await context.request.get('/api/users/me')).json();
    await page.goto('/profile');await expect(page.locator('article.post')).toHaveCount(20);
    await page.getByRole('button',{name:'Завантажити ще',exact:true}).click();
    await expect(page.locator('article.post')).toHaveCount(21);
    await expect(page.getByRole('button',{name:'Завантажити ще',exact:true})).toHaveCount(0);
    const other = await browser.newContext({baseURL:'http://localhost:15173'});
    try{
        const second = await other.newPage();await register(second);
        expect((await mutate(other.request,'/api/posts/'+ids[0],{method:'DELETE'})).status()).toBe(403);
        expect((await mutate(other.request,'/api/users/'+owner.id+'/follow',{method:'PUT'})).status()).toBe(204);
        await second.reload();await second.getByRole('tab',{name:'Following',exact:true}).click();
        await expect(second.locator('article.post')).toHaveCount(20);
        await second.getByRole('button',{name:'Завантажити ще',exact:true}).click();
        await expect(second.locator('article.post')).toHaveCount(21);
        const emptyScope = await other.request.get('/api/users/me/following/posts?pagination=cursor&cursor='+firstWindow.nextCursor);
        expect(emptyScope.status()).toBe(400);
    }finally{await other.close();}
});

test('Google OIDC redirects to password setup once, then email login and subsequent Google login work',async({page,context})=>{
    await page.goto('/login');
    const exchange = page.waitForRequest(request=>new URL(request.url()).pathname==='/api/auth/oauth/exchange'&&request.method()==='POST');
    await page.getByRole('button',{name:'Продолжить с Google',exact:true}).click();
    await expect(page).toHaveURL(/\/setup-password$/);
    const exchangedCode = (await exchange).postDataJSON().code;
    expect((await mutate(context.request,'/api/auth/oauth/exchange',{data:{code:exchangedCode}})).status()).toBe(400);
    expect(page.url()).not.toContain('code=');expect(page.url()).not.toContain('token=');
    await page.getByLabel('Новий пароль',{exact:true}).fill(password);
    await page.getByLabel('Повтори пароль',{exact:true}).fill(password);
    await page.getByRole('button',{name:'Зберегти та продовжити'}).click();
    await expect(page).toHaveURL(/\/setup-profile$/);
    await page.getByLabel('Username',{exact:true}).fill('google_e2e');
    await page.getByLabel('Видиме ім’я',{exact:true}).fill('Google E2E');
    await page.getByRole('button',{name:'Зберегти та продовжити'}).click();await expect(page).toHaveURL(/\/feed$/);
    await page.goto('/profile');await page.getByRole('button',{name:'Вийти з акаунту'}).click();
    await expect(page).toHaveURL(/\/login$/);
    await login(page,'google-e2e@example.test');
    expect((await context.request.get('/api/users/me/password')).status()).toBe(200);
    await page.goto('/profile');await page.getByRole('button',{name:'Вийти з акаунту'}).click();
    await expect(page).toHaveURL(/\/login$/);
    await page.getByRole('button',{name:'Продолжить с Google',exact:true}).click();
    await expect(page).toHaveURL(/\/feed$/);
});

test('public user ID cannot bypass password and the sixth email-code attempt is rejected',async({page,context})=>{
    const user = identity();const challenge = await beginLogin(page,user.email);
    const bypass = await mutate(context.request,'/api/auth/verify-2fa',{data:{userId:challenge.userId,code:challenge.code}});
    expect(bypass.status()).toBe(400);
    for(let i=0;i<5;i++){
        const response = await mutate(context.request,'/api/auth/verify-2fa',{data:{
            challengeId:challenge.challengeId,code:challenge.code==='000000'?'111111':'000000'
        }});expect(response.status()).toBe(400);
    }
    const exhausted = await mutate(context.request,'/api/auth/verify-2fa',{data:{challengeId:challenge.challengeId,code:challenge.code}});
    expect(exhausted.status()).toBe(429);
    expect((await context.request.get('/api/users/me')).status()).toBe(401);
});
