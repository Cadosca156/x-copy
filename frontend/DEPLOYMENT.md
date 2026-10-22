# Deployment: Amplify frontend and Lightsail backend

Frontend: https://main.d2s4oyee9a0d3l.amplifyapp.com

Backend: https://18-197-16-216.nip.io

## Google OAuth client

In Google Cloud Console, open the OAuth client matching GOOGLE_CLIENT_ID in the server's .env.
Add this exact Authorized redirect URI:

```text
https://18-197-16-216.nip.io/login/oauth2/code/google
```

Keep http://localhost:8080/login/oauth2/code/google if local development is needed.
If the Google consent screen is in Testing mode, add the intended Google accounts as test users.
Do not enter the frontend /oauth2/success URL as the Google redirect URI: Spring handles the Google callback and then redirects to the frontend.

## Configuration

frontend/.env.production sets the public Google login base URL. It contains no credentials.
VITE_OAUTH_BASE_URL in the Amplify build environment can override it; rebuild after changing it.

Docker Compose defaults to the deployed frontend and callback. Optional overrides in the server's root .env:

```dotenv
FRONTEND_URL=https://main.d2s4oyee9a0d3l.amplifyapp.com
GOOGLE_REDIRECT_URI=https://18-197-16-216.nip.io/login/oauth2/code/google
```

For local Docker development, use FRONTEND_URL=http://localhost:5173, GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google and SESSION_COOKIE_SECURE=false in the root .env when using HTTP.

Caddy must continue proxying to 127.0.0.1:8080. Only backend's 127.0.0.1:8080 is published; the databases communicate over the default Compose network. Uploaded media uses the existing media_data volume. Existing database and media volume names are unchanged.

## Apply updates

Commit and push all changed files, including backend files and frontend/.env.production. Amplify rebuilds the frontend.
On the Lightsail server, run each command in order; stop if any command fails:

```bash
cd ~/x-copy
git pull
cd backend
./gradlew test --tests 'com.net.cardo.xcopybackend.config.ProductionLinksTest' --tests 'com.net.cardo.xcopybackend.config.ApiCorsConfigTest' bootJar
cd ..
sudo docker compose --env-file .env up -d --build
sudo docker compose --env-file .env ps
```

Use up without --no-deps so changed database port settings are applied too. Do not remove volumes.
If git pull reports conflicts with server edits to Compose, resolve them while retaining the single localhost backend port and database volumes.

## Verify

```bash
curl --max-time 15 -i https://18-197-16-216.nip.io/api/users/me
```

Expect 401 without a token. Then verify email login, Google login, and the password-reset flow in the browser. Newly generated reset emails must link to the Amplify domain. Older emails still contain the previous URL.

In Lightsail networking, keep TCP 80 and 443 available for Caddy. Restrict SSH to administrative access; remove any public rules for 5432, 27017, 6379, 8080 or 18080. Check both IPv4 and IPv6 rule sets where enabled.

An actual Google login and mail delivery must be checked against the deployed app; local automated tests mock these external services.
