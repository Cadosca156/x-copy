#!/usr/bin/env bash
set -euo pipefail

for variable in LIGHTSAIL_HOST LIGHTSAIL_USER LIGHTSAIL_SSH_KEY LIGHTSAIL_KNOWN_HOSTS; do
  if [[ -z "${!variable:-}" ]]; then
    echo "::error::Missing GitHub secret: $variable"
    exit 1
  fi
done
[[ "$LIGHTSAIL_HOST" =~ ^[a-zA-Z0-9][a-zA-Z0-9.-]*$ ]] || { echo 'Invalid server hostname'; exit 1; }
[[ "$LIGHTSAIL_USER" =~ ^[a-z_][a-z0-9_-]*$ ]] || { echo 'Invalid SSH username'; exit 1; }
[[ "${GITHUB_SHA:-}" =~ ^[0-9a-f]{40}$ ]] || { echo 'Invalid deployment commit'; exit 1; }

work_dir=$(mktemp -d)
trap 'rm -rf -- "$work_dir"' EXIT
umask 077
printf '%s\n' "$LIGHTSAIL_SSH_KEY" | tr -d '\r' > "$work_dir/key"
printf '%s\n' "$LIGHTSAIL_KNOWN_HOSTS" | tr -d '\r' > "$work_dir/known_hosts"
ssh-keygen -y -f "$work_dir/key" > /dev/null
ssh-keygen -F "$LIGHTSAIL_HOST" -f "$work_dir/known_hosts" > /dev/null

shopt -s nullglob
jars=(deploy-artifact/*.jar)
[[ ${#jars[@]} -eq 1 ]] || { echo 'Expected exactly one tested application JAR'; exit 1; }
mkdir -p "$work_dir/bundle/backend/build/libs"
cp "${jars[0]}" "$work_dir/bundle/backend/build/libs/application.jar"
cp backend/Dockerfile "$work_dir/bundle/backend/Dockerfile"
cp docker-compose.yaml "$work_dir/bundle/docker-compose.yaml"
cp .github/scripts/deploy-on-server.sh "$work_dir/bundle/deploy-on-server.sh"
cp .github/scripts/configure-jwt-secret.sh "$work_dir/bundle/configure-jwt-secret.sh"
tar -czf "$work_dir/release.tar.gz" -C "$work_dir/bundle" .

ssh_options=(-i "$work_dir/key" -o BatchMode=yes -o IdentitiesOnly=yes
  -o StrictHostKeyChecking=yes -o "UserKnownHostsFile=$work_dir/known_hosts"
  -o ConnectTimeout=15 -o ServerAliveInterval=15 -o ServerAliveCountMax=4)
target="$LIGHTSAIL_USER@$LIGHTSAIL_HOST"
remote_dir=".cache/xcopy-deploy/$GITHUB_SHA"
ssh "${ssh_options[@]}" "$target" "umask 077; mkdir -p '$remote_dir'"
scp "${ssh_options[@]}" "$work_dir/release.tar.gz" "$target:$remote_dir/release.tar.gz"
ssh "${ssh_options[@]}" "$target" \
  "set -e; cd '$remote_dir'; tar -xzf release.tar.gz; timeout 900 bash deploy-on-server.sh '$GITHUB_SHA'"

echo "Backend deployment verified for $GITHUB_SHA"
