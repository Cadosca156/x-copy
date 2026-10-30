These E2E tests run the real frontend, executable Spring backend, PostgreSQL, MongoDB, Redis and SMTP delivery to Mailpit. They do not mock application API responses. Accounts, posts and files are disposable; no production endpoint or credentials are used.

Google's external service is replaced by the local test OIDC provider, including discovery, authorization redirects, one-use authorization codes, signed ID tokens, JWKS and userinfo. This exercises the application's OAuth/OIDC integration and password setup, but does not verify Google's live login/consent screens or its production client configuration.

Requirements: Docker, Node.js and a Java toolchain capable of building the backend. Set JAVA_HOME when Java is not on PATH. On Windows the browser is installed Edge; on Linux install Chromium with `npx playwright install --with-deps chromium`.

Run from the repository root:

```sh
docker compose -p xcopy-e2e -f frontend/e2e/compose.yaml up --detach --wait --wait-timeout 150
cd backend
./gradlew bootJar
cd ../frontend
npm ci
npm run test:e2e
```

PowerShell uses `.\gradlew.bat bootJar`. On Windows, add Docker's bin directory to PATH if its credential helper cannot be found when pulling images.

The runner starts and stops the frontend, backend and OIDC provider. It uses dedicated ports 15173/18080/19090, test-only database ports and a new MongoDB database per invocation. The backend recreates the isolated PostgreSQL schema; setup flushes only Redis in Compose project xcopy-e2e. Do not point this configuration at existing services or change these URLs to production.

Five browser scenarios cover:

- Email registration, delivered verification code, profile setup, file upload/display, posts, likes/comments, deletion, logout and rejection of the former session.
- Recovery mail, identical known/unknown-email responses, reset-link reuse rejection, old-session/password rejection and successful new-password login.
- Database pagination, rejection of missing CSRF tokens and forged image content, and rejection of deleting another account's post.
- OIDC login with password/profile setup, replay rejection for the handoff code, subsequent email login and subsequent OIDC login.
- Rejection of public-user-ID login bypass and exhaustion of the code-attempt budget.

Tests also fail on uncaught page errors or unexpected HTTP 5xx responses from application APIs. HTML output is in playwright-report; failure traces/screenshots are in test-results. CI saves these as an artifact and requires E2E success before production deployment.

After local testing, stop the disposable services from the repository root:

```sh
docker compose -p xcopy-e2e -f frontend/e2e/compose.yaml down --volumes --remove-orphans
```
