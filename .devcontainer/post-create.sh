#!/usr/bin/env bash
# Runs once after the Codespace container is created.
set -euo pipefail

echo "==> Enabling auto-format git hook"
git config core.hooksPath .githooks || true

echo "==> Installing 'just' task runner (if missing)"
if ! command -v just >/dev/null 2>&1; then
  curl --proto '=https' --tlsv1.2 -sSf https://just.systems/install.sh \
    | sudo bash -s -- --to /usr/local/bin
fi

# --- Generate .env from Codespaces secrets (never overwrite an existing one) ---
# upsert KEY=VALUE in .env: replace the line if present, append if not.
upsert() {
  local key="$1" val="${2:-}"
  [ -z "$val" ] && return 0
  if grep -q "^${key}=" .env; then
    # use a delimiter unlikely to appear in secrets
    sed -i "s|^${key}=.*|${key}=${val}|" .env
  else
    printf '%s=%s\n' "$key" "$val" >> .env
  fi
}

if [ ! -f .env ]; then
  echo "==> Creating .env from .env.example"
  cp .env.example .env
  upsert NEO4J_PASSWORD    "${NEO4J_PASSWORD:-}"
  upsert OPENAI_API_KEY    "${OPENAI_API_KEY:-}"
  upsert VOYAGEAI_API_KEY  "${VOYAGEAI_API_KEY:-}"
  upsert API_KEY           "${API_KEY:-}"
else
  echo "==> .env already exists, leaving it untouched"
fi

echo "==> Warming the Maven dependency cache (offline resolve)"
./mvnw -B -q -DskipTests dependency:go-offline || true

echo "==> Done. Run 'just up' to start the stack, or 'just dev' for the app only."