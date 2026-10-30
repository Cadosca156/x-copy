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
    await register(page);
    const cookies = await context.cookies();const session = cookies.find(cookie=>cookie.name==='XCOPY_SESSION');
    expect(session?.httpOnly).toBe(true);expect(session.sameSite).toBe('Lax');
    expect(await page.evaluate(()=>localStorage.getItem('token'))).toBeNull();
    expect(await page.evaluate(()=>document.cookie)).not.toContain('XCOPY_SESSION');
    const content = 'Real E2E post '+Date.now();
    await page.getByRole('button',{name:'Post',exact:true}).click();
    await page.getByPlaceholder('What is happening?!').fill(content);
    await page.getByLabel('Додати фото або відео').setInputFiles({
        name:'pixel.png',mimeType:'image/png',
        buffer:Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jE1sAAAAASUVORK5CYII=','base64')
    });
    await page.locator('.composer-modal').getByRole('button',{name:'Post',exact:true}).click();
    const post = page.locator('article.post').filter({has:page.getByText(content,{exact:true})});
    await expect(post).toBeVisible();
    const image = post.locator('img.post__image');await expect(image).toBeVisible();
    await expect.poll(()=>image.evaluate(image=>image.complete&&image.naturalWidth>0)).toBe(true);
    await post.getByRole('button',{name:'Like post',exact:true}).click();
    await expect(post.getByRole('button',{name:'Remove like',exact:true})).toBeVisible();
    await post.locator('.post-action--reply').click();
    await post.getByLabel('New comment').fill('E2E comment');
    await post.locator('.comment-form button[type=submit]').click();
    await expect(post.getByText('E2E comment',{exact:true})).toBeVisible();
    page.once('dialog',dialog=>dialog.accept());
    await post.locator('.comment').getByRole('button',{name:'Видалити',exact:true}).click();
    await expect(post.getByText('E2E comment',{exact:true})).toHaveCount(0);
    await post.getByRole('button',{name:'More post options'}).click();
    page.once('dialog',dialog=>dialog.accept());
    await post.getByRole('menuitem',{name:'Видалити',exact:true}).click();await expect(post).toHaveCount(0);
    await page.goto('/profile');
    await page.getByRole('button',{name:'Вийти з акаунту'}).click();await expect(page).toHaveURL(/\/login$/);
    const replay = await context.request.get('/api/users/me',{headers:{Cookie:'XCOPY_SESSION='+session.value}});
    expect(replay.status()).toBe(401);
});

test('password reset uses real mail, revokes old session, rejects reuse and permits the new password',async({page,context})=>{
    const user = await register(page);
    const oldSession = (await context.cookies()).find(cookie=>cookie.name==='XCOPY_SESSION').value;
    await page.goto('/reset-password-email');
    await page.getByLabel('Email',{exact:true}).fill(user.email);
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
    const revoked = await context.request.get('/api/users/me',{headers:{Cookie:'XCOPY_SESSION='+oldSession}});
    expect(revoked.status()).toBe(401);
    const reuse = await mutate(context.request,'/api/auth/password/reset',{data:{token,newPassword:'Another-password-789'}});
    expect(reuse.status()).toBe(400);
    await page.goto('/login');
    await page.locator('input[type=email]').fill(user.email);await page.locator('input[type=password]').fill(password);
    await page.getByRole('button',{name:'Продолжить',exact:true}).click();
    await expect(page.getByRole('alert')).toContainText(/парол|email|емейл/i);
    await login(page,user.email,newPassword);await expect(page).toHaveURL(/\/feed$/);
});

test('real database pagination, CSRF rejection, spoofed upload and post ownership',async({page,context,browser})=>{
    await register(page);
    expect((await context.request.post('/api/posts',{multipart:{content:'No CSRF'}})).status()).toBe(403);
    const spoofed = await mutate(context.request,'/api/posts',{multipart:{
        content:'Spoofed upload',file:{name:'photo.png',mimeType:'image/png',buffer:Buffer.from('<script>spoof</script>')}
    }});expect(spoofed.status()).toBe(415);
    const ids = [];
    for(let i=0;i<21;i++){
        const response = await mutate(context.request,'/api/posts',{multipart:{content:'Pagination E2E '+i}});
        expect(response.status()).toBe(201);ids.push((await response.json()).id);
    }
    await page.reload();await expect(page.locator('article.post')).toHaveCount(20);
    await page.getByRole('button',{name:'Завантажити ще',exact:true}).click();
    await expect(page.locator('article.post')).toHaveCount(21);
    expect((await context.request.get('/api/posts?page=0&size=100000')).status()).toBe(400);
    const other = await browser.newContext({baseURL:'http://localhost:15173'});
    try{
        const second = await other.newPage();await register(second);
        expect((await mutate(other.request,'/api/posts/'+ids[0],{method:'DELETE'})).status()).toBe(403);
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
