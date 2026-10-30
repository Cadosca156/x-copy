// Test-only OpenID Connect provider. Never used by the production application.
import http from 'node:http';
import {generateKeyPairSync,randomBytes,sign} from 'node:crypto';
const issuer = 'http://127.0.0.1:19090';
const {privateKey,publicKey} = generateKeyPairSync('rsa',{modulusLength:2048});
const publicJwk = {...publicKey.export({format:'jwk'}),kid:'e2e-key',alg:'RS256',use:'sig'};
const codes = new Map();
const identity = {sub:'google-e2e-subject',email:'google-e2e@example.test',email_verified:true,name:'Google E2E'};
function json(response,data,status=200) {
    response.writeHead(status,{'content-type':'application/json','cache-control':'no-store'});
    response.end(JSON.stringify(data));
}
function jwt(nonce) {
    const now = Math.floor(Date.now()/1000);
    const header = Buffer.from(JSON.stringify({alg:'RS256',kid:'e2e-key',typ:'JWT'})).toString('base64url');
    const claims = Buffer.from(JSON.stringify({...identity,iss:issuer,aud:'e2e-client',iat:now,exp:now+300,nonce})).toString('base64url');
    const payload = header+'.'+claims;
    return payload+'.'+sign('RSA-SHA256',Buffer.from(payload),privateKey).toString('base64url');
}
http.createServer(async(request,response)=>{
    const url = new URL(request.url,issuer);
    if(url.pathname==='/.well-known/openid-configuration')return json(response,{
        issuer,authorization_endpoint:issuer+'/authorize',token_endpoint:issuer+'/token',
        userinfo_endpoint:issuer+'/userinfo',jwks_uri:issuer+'/jwks',
        response_types_supported:['code'],subject_types_supported:['public'],
        id_token_signing_alg_values_supported:['RS256'],scopes_supported:['openid','profile','email'],
        token_endpoint_auth_methods_supported:['client_secret_basic']
    });
    if(url.pathname==='/jwks')return json(response,{keys:[publicJwk]});
    if(url.pathname==='/authorize') {
        const redirect = new URL(url.searchParams.get('redirect_uri'));
        if(redirect.origin!=='http://localhost:18080' || redirect.pathname!=='/login/oauth2/code/google')
            return json(response,{error:'invalid_redirect_uri'},400);
        const code = randomBytes(32).toString('base64url');
        codes.set(code,{nonce:url.searchParams.get('nonce'),expires:Date.now()+60000});
        redirect.searchParams.set('code',code);redirect.searchParams.set('state',url.searchParams.get('state'));
        response.writeHead(302,{location:redirect.href});return response.end();
    }
    if(url.pathname==='/token') {
        if(request.headers.authorization!=='Basic '+Buffer.from('e2e-client:e2e-secret').toString('base64'))
            return json(response,{error:'invalid_client'},401);
        let body='';for await(const chunk of request)body+=chunk;
        const code = new URLSearchParams(body).get('code'), entry = codes.get(code);codes.delete(code);
        if(!entry || entry.expires<Date.now())return json(response,{error:'invalid_grant'},400);
        return json(response,{access_token:'e2e-access',token_type:'Bearer',expires_in:300,id_token:jwt(entry.nonce),scope:'openid profile email'});
    }
    if(url.pathname==='/userinfo') {
        if(request.headers.authorization!=='Bearer e2e-access')return json(response,{error:'invalid_token'},401);
        return json(response,identity);
    }
    json(response,{ok:true});
}).listen(19090,'127.0.0.1');
