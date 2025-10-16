#!/usr/bin/env bash
set -euo pipefail

# Simple local key rotation helper.
# - Writes new secrets into a local .env (never commits)
# - Prints next steps for updating remote envs

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/.. && pwd)"
ENV_FILE="$ROOT_DIR/.env"

random_b64() { # length in bytes -> base64
  openssl rand -base64 "$1"
}
random_hex() { # length in bytes -> hex
  openssl rand -hex "$1"
}

JWT_SECRET=$(random_b64 48)
SUPABASE_JWT_SECRET=$(random_b64 48)
SUPABASE_ANON_KEY_PLACEHOLDER=$(random_hex 48)
SUPABASE_SERVICE_ROLE_KEY_PLACEHOLDER=$(random_hex 64)

# Create or update .env
if [[ ! -f "$ENV_FILE" ]]; then
  touch "$ENV_FILE"
fi

# Safely update or append keys
upsert_env() {
  local key="$1" value="$2"
  if grep -q "^${key}=" "$ENV_FILE" 2>/dev/null; then
    sed -i.bak "s|^${key}=.*|${key}=${value}|" "$ENV_FILE" && rm -f "$ENV_FILE.bak"
  else
    echo "${key}=${value}" >> "$ENV_FILE"
  fi
}

upsert_env JWT_SECRET "$JWT_SECRET"
upsert_env SUPABASE_JWT_SECRET "$SUPABASE_JWT_SECRET"
# Placeholders until you paste real keys from Supabase Dashboard
upsert_env SUPABASE_ANON_KEY "$SUPABASE_ANON_KEY_PLACEHOLDER"
upsert_env SUPABASE_SERVICE_ROLE_KEY "$SUPABASE_SERVICE_ROLE_KEY_PLACEHOLDER"

cat <<EOF
Local .env updated at: $ENV_FILE

IMPORTANT:
- Replace SUPABASE_ANON_KEY and SUPABASE_SERVICE_ROLE_KEY placeholders with the real regenerated values from Supabase Dashboard (Project Settings → API).
- If JWT secret is managed by Supabase, paste SUPABASE_JWT_SECRET there as well.
- Update deployments/CI secrets to match.

Next steps:
1) Open SECURITY_ROTATION_GUIDE.md and follow steps 1 and 5.
2) Rebuild/restart services so they pick up new secrets.
EOF
