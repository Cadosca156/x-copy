#!/usr/bin/env bash
set -euo pipefail
env_file=${1:?Pass the application .env path}
[[ -f "$env_file" && ! -L "$env_file" ]] || { echo 'Missing regular server .env file'; exit 1; }
umask 077
chmod 600 "$env_file"
count=$(grep -c '^JWT_SECRET=' "$env_file" || true)
if [[ "$count" == 0 ]]; then
  secret=$(openssl rand -base64 64 | tr -d '\r\n')
  printf '\nJWT_SECRET=%s\n' "$secret" >> "$env_file"
  unset secret
  echo 'Created a private JWT signing key; previous sessions will require login.'
elif [[ "$count" != 1 ]]; then
  echo 'Expected exactly one JWT_SECRET entry in server .env'; exit 1
fi
secret=$(sed -n 's/^JWT_SECRET=//p' "$env_file" | tr -d '\r')
[[ "$secret" =~ ^[A-Za-z0-9+/]+={0,2}$ ]] || { echo 'JWT_SECRET must be an unquoted Base64 value'; exit 1; }
bytes=$(printf '%s' "$secret" | openssl base64 -d -A | wc -c)
[[ "$bytes" -ge 32 ]] || { echo 'JWT_SECRET must contain at least 32 random bytes'; exit 1; }
unset secret
