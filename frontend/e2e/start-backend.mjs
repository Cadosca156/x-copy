import {spawn} from 'node:child_process';
import {randomBytes} from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
const backend = path.resolve('../backend');
const jar = fs.readdirSync(path.join(backend,'build/libs')).find(name=>name.endsWith('.jar')&&!name.endsWith('-plain.jar'));
if(!jar)throw new Error('Build the backend JAR before running E2E: gradlew bootJar');
const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME,'bin',process.platform==='win32'?'java.exe':'java') : 'java';
const child = spawn(java,['-Duser.timezone=UTC','-jar',path.join(backend,'build/libs',jar)],{
    cwd:backend,stdio:'inherit',env:{...process.env,
        SERVER_PORT:'18080',SPRING_DATASOURCE_URL:'jdbc:postgresql://localhost:15432/xcopy_e2e',
        LOGGING_LEVEL_ROOT:'WARN',SPRING_MAIN_BANNER_MODE:'off',
        PSQL_USER:'xcopy_e2e',PSQL_PASSWORD:'e2e-only-password',
        MONGODB_URI:'mongodb://localhost:27018/xcopy_e2e_'+randomBytes(6).toString('hex'),REDIS_HOST:'localhost',REDIS_PORT:'16379',
        SPRING_JPA_HIBERNATE_DDL_AUTO:'create-drop',SPRING_JPA_SHOW_SQL:'false',
        APP_JWT_SECRET:randomBytes(64).toString('base64'),APP_AUTH_SECURE_COOKIES:'false',
        APP_FRONTEND_URL:'http://localhost:15173',APP_CORS_ALLOWED_ORIGINS:'http://localhost:15173',
        APP_MEDIA_UPLOAD_DIR:path.resolve('test-results/uploads'),
        MAIL_USERNAME:'e2e@example.test',MAIL_PASSWORD:'e2e-only-password',
        SPRING_MAIL_HOST:'localhost',SPRING_MAIL_PORT:'11025',
        SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH:'false',SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE:'false',
        GOOGLE_CLIENT_ID:'e2e-client',GOOGLE_CLIENT_SECRET:'e2e-secret',
        SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_GOOGLE_ISSUER_URI:'http://127.0.0.1:19090',
        SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_GOOGLE_AUTHORIZATION_URI:'http://127.0.0.1:19090/authorize',
        SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_GOOGLE_TOKEN_URI:'http://127.0.0.1:19090/token',
        SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_GOOGLE_USER_INFO_URI:'http://127.0.0.1:19090/userinfo',
        SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_GOOGLE_JWK_SET_URI:'http://127.0.0.1:19090/jwks',
        SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_REDIRECT_URI:'http://localhost:18080/login/oauth2/code/google',
        SERVER_SERVLET_SESSION_COOKIE_SECURE:'false',
    }
});
child.on('error',error=>{console.error(error);process.exitCode=1;});
child.on('exit',code=>process.exit(code??1));
for(const signal of ['SIGINT','SIGTERM'])process.on(signal,()=>child.kill(signal));
