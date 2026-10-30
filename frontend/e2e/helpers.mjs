import {expect} from '@playwright/test';
import {randomUUID} from 'node:crypto';
export const password = 'E2e-password-123';
export const identity = () => {
    const id = randomUUID().slice(0,8);
    return {email:'e2e-'+id+'@example.test',username:'e2e_'+id,name:'E2E '+id};
};
export async function mutate(request,path,options={}) {
    const response = await request.get('/api/auth/csrf');
    expect(response.ok()).toBeTruthy();
    const csrf = await response.json();
    return request.fetch(path,{method:'POST',...options,headers:{...options.headers,'X-XSRF-TOKEN':csrf.token}});
}
export async function waitForMail(email,subject,ignored=new Set()) {
    let message;
    await expect.poll(async()=>{
        const response = await fetch('http://localhost:18025/api/v1/messages');
        const data = await response.json();
        message = data.messages.find(item=>item.Subject===subject && !ignored.has(item.ID) &&
            item.To.some(to=>to.Address.toLowerCase()===email.toLowerCase()));
        return !!message;
    },{timeout:20000,message:'The backend must deliver real SMTP mail to Mailpit'}).toBe(true);
    return (await fetch('http://localhost:18025/api/v1/message/'+message.ID)).json();
}
export async function beginLogin(page,email,secret=password) {
    const messages = await (await fetch('http://localhost:18025/api/v1/messages')).json();
    const previous = new Set(messages.messages.map(message=>message.ID));
    await page.goto('/login');
    await page.locator('input[type=email]').fill(email);
    await page.locator('input[type=password]').fill(secret);
    const response = page.waitForResponse(response=>new URL(response.url()).pathname==='/api/auth/login' && response.request().method()==='POST');
    await page.getByRole('button',{name:'Продолжить',exact:true}).click();
    const result = await response;expect(result.status()).toBe(200);
    await page.waitForURL('**/2fa');
    const mail = await waitForMail(email,'Verification code',previous);
    const code = mail.Text.match(/\b\d{6}\b/)[0];
    return {...await result.json(),code};
}
export async function login(page,email,secret=password) {
    const challenge = await beginLogin(page,email,secret);
    await page.getByLabel('Code digit 1',{exact:true}).fill(challenge.code);
    await page.locator('form').evaluate(form=>form.requestSubmit());
    await expect(page).toHaveURL(/\/(feed|setup-profile)$/);
    return challenge;
}
export async function register(page) {
    const user = identity();await login(page,user.email);
    await expect(page).toHaveURL(/\/setup-profile$/);
    await page.getByLabel('Username',{exact:true}).fill(user.username);
    await page.getByLabel('Видиме ім’я',{exact:true}).fill(user.name);
    await page.getByRole('button',{name:'Зберегти та продовжити'}).click();
    await expect(page).toHaveURL(/\/feed$/);
    return user;
}
