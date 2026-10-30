Security changes prepared on codex/security-hardening
===================================================

The previous public JWT signing key must be considered compromised. The backend now requires an external Base64 key containing at least 32 random bytes. Deployment creates a 64-byte random key in ~/x-copy/.env when JWT_SECRET is absent, without printing it; subsequent deployments preserve it. An invalid or duplicate entry stops deployment before replacing the running application. Existing sessions will require login after the first release. Rotate an existing key if it was ever exposed.

Implemented changes
-------------------

1. Removed the embedded signing key and added startup validation.
2. Email codes now require a short-lived opaque challenge created only after a correct password or registration. Public user IDs cannot request or verify codes. Codes are hashed, expire in five minutes, and are consumed atomically.
3. Redis enforces shared limits: ten password attempts per email per fifteen minutes, five code emails per account per fifteen minutes, five verification attempts per challenge, and one resend per minute. Resends retain the challenge expiry and attempt counter. Authentication also has IP limits. Mutating application requests have an IP limit. Redis failure does not bypass these controls.
4. JWTs bind both the immutable user ID and a database session version. Password reset and logout increment the version; recreating an email does not restore old tokens. Reset invalidates the user's other reset links. Logout currently ends all sessions for that account.
5. Browser sessions use Secure, HttpOnly, SameSite=Lax cookies. JWTs are absent from redirect URLs, response JSON, localStorage and frontend bearer headers. Google redirects carry a one-use, sixty-second code bound to the initiating browser. The callback immediately removes that code from the address bar. Mutations require a CSRF token. Google accounts without a password still go through password setup.
6. Uploads are limited to 25 MB each, 500 MB/500 files per user and 5 GB/10,000 files globally, with 512 MB of disk space reserved. Quota checks and writes share a process/file lock. Filenames are generated, uploads are grouped by authenticated user ID, and supported file signatures are checked. Signature checks are not antivirus scanning. Legacy flat files count toward the global quota. Post/comment text is bounded; feeds and comments return twenty entries by default, at most fifty, with bounded page offsets and load-more controls.
7. Password-recovery requests return the same empty HTTP 202 for known and unknown email addresses. Lookup, token creation and mail delivery run in a bounded background queue, removing account-dependent SMTP timing from the public response. Recovery email has an additional per-address limit.

Release and verification
------------------------

Merge only after frontend and backend CI checks pass. The existing main-branch workflow deploys the backend; Amplify must deploy the matching frontend. During the transition, old frontend sessions are incompatible with the new login contract, so refresh the page and sign in again.

The existing Amplify /api proxy must forward Set-Cookie and Cookie headers without caching authentication responses. Verify on the deployed site that /api/auth/csrf sets XSRF-TOKEN and is not cached, email login sets XCOPY_SESSION with Secure and HttpOnly, authenticated /api/users/me succeeds, Google login offers password setup where required, and logout makes the former session fail with HTTP 401.

For local HTTP-only development, explicitly set APP_AUTH_SECURE_COOKIES=false on the backend. Production keeps the default true. Never use the fixed CI signing key outside tests.

Local verification includes backend unit tests and the actual Spring Security filter chain, frontend tests/lint/build, deployment-key creation/idempotence/rejection checks, and browser E2E scenarios against the real frontend, backend, PostgreSQL, MongoDB, Redis and SMTP delivery to Mailpit. E2E uses a test OpenID Connect provider for the Google integration; Google's live login/consent and production client settings remain a deployment check. CI additionally tests real Redis atomic operations, application startup and the browser E2E suite.

Scope: these fixes address the seven code/configuration findings. They do not constitute an audit of Lightsail firewall rules, Caddy, Amplify proxy settings, backups, administrator access or every dependency vulnerability.

Repeatable unit checks
----------------------

Run `./gradlew unitTest` in backend (PowerShell: `.\gradlew.bat unitTest`) and `npm test` in frontend. The backend unit task excludes the application/Redis integration tests and the two Spring-context security tests; it requires no database, Redis or SMTP service. The normal `test`/`check` tasks still include the complete suite in CI.

Adversarial unit coverage includes expired/wrong-issuer and legacy JWTs, deleted accounts, password-reset races, account/IP throttling, Redis failure, expired/reset email-code challenges, attempt budgets across resends, concurrent challenge consumption, missing Google browser binding, revoked handoff sessions, reset-link/session invalidation, uniform recovery responses, byte/file quotas, dishonest upload lengths with partial-file cleanup, bounded database queries and oversized text rejection. Frontend unit checks cover password-setup routing, password validation and the cookie/CSRF request contract.

Repeatable E2E checks
---------------------

See [e2e/README.md](e2e/README.md). After starting the disposable E2E services and building the backend JAR, run `npm run test:e2e` in frontend. All five scenarios must pass before CI allows the production deployment job to run.
