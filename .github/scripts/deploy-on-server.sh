#!/usr/bin/env bash
set -euo pipefail

commit=${1:-}
[[ "$commit" =~ ^[0-9a-f]{40}$ ]] || { echo 'Invalid deployment commit'; exit 1; }
release_dir=$(pwd -P)
app_dir="$HOME/x-copy"
[[ -f "$app_dir/.env" ]] || { echo 'Missing server configuration: ~/x-copy/.env'; exit 1; }
[[ -f backend/build/libs/application.jar && -f backend/Dockerfile && -f docker-compose.yaml ]] || {
  echo 'Incomplete deployment bundle'; exit 1;
}

# Also serialize deployments started outside GitHub Actions.
exec 9> "$HOME/.cache/xcopy-deploy/deploy.lock"
flock -w 60 9 || { echo 'Another deployment is running'; exit 1; }
bash "$release_dir/configure-jwt-secret.sh" "$app_dir/.env"
sudo -n docker info > /dev/null
sudo -n docker compose version
command -v curl > /dev/null

# Preserve the existing Compose project and volumes rooted at ~/x-copy.
export XCOPY_IMAGE_TAG="$commit"
cat > compose.ci.yaml <<'YAML'
services:
  backend:
    image: xcopybackend:${XCOPY_IMAGE_TAG}
YAML
compose=(sudo -n env "XCOPY_IMAGE_TAG=$commit" docker compose
  --project-directory "$app_dir" --env-file "$app_dir/.env"
  -f "$release_dir/docker-compose.yaml" -f "$release_dir/compose.ci.yaml")
"${compose[@]}" config --quiet
sudo -n docker build --tag "xcopybackend:$commit" "$release_dir/backend"
"${compose[@]}" up -d --no-build backend

# The protected endpoint returns 401 when the application is ready without a JWT.
for attempt in {1..36}; do
  status=$(curl --silent --output /dev/null --write-out '%{http_code}' \
    --connect-timeout 2 --max-time 5 http://127.0.0.1:8080/api/users/me) || status=000
  if [[ "$status" == 401 ]]; then
    printf '%s\n' "$commit" > "$HOME/.cache/xcopy-deploy/current-commit"
    "${compose[@]}" ps
    echo "Backend ready: HTTP 401 from /api/users/me ($commit)"
    exit 0
  fi
  sleep 5
done

"${compose[@]}" ps
echo 'Backend readiness check failed; inspect the service on Lightsail.'
exit 1
