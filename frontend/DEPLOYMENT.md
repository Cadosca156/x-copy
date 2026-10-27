# Deployment: Amplify frontend and Lightsail backend

Frontend: https://main.d2s4oyee9a0d3l.amplifyapp.com

Backend: https://18-197-16-216.nip.io

## Continuous integration

`.github/workflows/ci.yml` runs on every push and pull request, and can also be
started manually from GitHub Actions.

Frontend and deployment jobs use `ubuntu-24.04` explicitly. Backend checks use
`ubuntu-22.04` because newer Ubuntu 24/26 runner images can use Linux kernels
6.19 or newer, which the current MongoDB image rejects (MongoDB SERVER-121912).
Revisit the backend runner once MongoDB supports those kernels, before Ubuntu 22
runners reach end of support.

- `Frontend checks`: Node.js 24, `npm ci`, ESLint with zero warnings, and the
  production Vite build.
- `Backend checks`: Java 21 runs Gradle 9, Java 17 compiles the application,
  and `check bootJar` runs all tests and builds the executable JAR. Temporary
  PostgreSQL 16, MongoDB 8, and Redis 7 services support the application context
  test. All credentials in this job are disposable CI values; GitHub Secrets
  and production services are not required.

Commit and push the workflow together with the accompanying source fixes to
activate these checks on GitHub. Their results appear under Actions and on pull
requests. Pushes to `main` also deploy the backend after both checks pass.
To require passing checks before merging, configure branch protection separately
with the `Frontend checks` and `Backend checks` statuses after their first run.

## Automatic backend deployment

The `Deploy backend to Lightsail` job runs only for `main`, after both CI jobs
succeed. It also runs when the workflow is manually started on `main`. Pull
requests and other branches only run checks. Changes developed on `Func` must
be merged into `main` to deploy.

Create these repository secrets under Settings > Secrets and variables > Actions:

| Secret | Value |
| --- | --- |
| `LIGHTSAIL_HOST` | `18.197.16.216` (update if the public IP changes) |
| `LIGHTSAIL_USER` | `ubuntu` |
| `LIGHTSAIL_SSH_KEY` | Complete private deployment key, without a passphrase |
| `LIGHTSAIL_KNOWN_HOSTS` | Trusted server public host key in known_hosts format |

Obtain the last value in the authenticated Lightsail browser SSH terminal:

```bash
printf '18.197.16.216 '; cut -d ' ' -f 1,2 /etc/ssh/ssh_host_ed25519_key.pub
```

Copy the single output line into `LIGHTSAIL_KNOWN_HOSTS`. This is a public host
key, not the deployment private key. Deployment requires strict host verification.

The server must have the existing `~/x-copy/.env`, Docker with the Compose plugin,
`curl`, `flock`, and passwordless access to the Docker commands via `sudo -n`.
Verify in the server terminal:

```bash
test -f ~/x-copy/.env && echo 'Server configuration exists'
sudo -n docker info > /dev/null && echo 'Docker access works'
sudo -n docker compose version
command -v curl flock timeout
```

SSH on port 22 must be reachable from the GitHub runner. If the Lightsail firewall
only allows your home IP, use a runner with a fixed permitted IP or a runner
inside the server network before expecting hosted GitHub runners to connect.

CI uploads the tested executable JAR and transfers it with the matching Dockerfile
and Compose configuration over SSH. Releases live in `~/.cache/xcopy-deploy/<SHA>`.
The server builds the Docker image from that bundle, keeps the existing Compose
project directory `~/x-copy` and its database/media volumes, and updates the backend
and its dependencies with `up -d --no-build backend`. No GitHub credentials or
Gradle installation are needed on the server. The server's Git checkout is not
updated by this process; the release bundle supplies the deployed configuration.

The job waits for `http://127.0.0.1:8080/api/users/me` to return the expected 401.
A failed deployment makes the Actions job fail. Automatic rollback is not configured.
Deployments run one at a time and new pushes do not interrupt an active deployment.
Old release directories and Docker images are retained and require occasional cleanup.

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
