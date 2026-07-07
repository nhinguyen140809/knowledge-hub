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

echo "==> Installing Claude Code CLI (if missing)"
if ! command -v claude >/dev/null 2>&1; then
  npm install -g @anthropic-ai/claude-code
fi

echo "==> Installing d2 (diagram renderer) (if missing)"
if ! command -v d2 >/dev/null 2>&1; then
  curl -fsSL https://d2lang.com/install.sh | sh -s --
fi

echo "==> Installing rsvg-convert for SVG->PDF diagram export (if missing)"
if ! command -v rsvg-convert >/dev/null 2>&1; then
  sudo apt-get update -qq && sudo apt-get install -y -qq librsvg2-bin
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
  upsert NEO4J_PASSWORD     "${NEO4J_PASSWORD:-}"
  upsert API_KEY            "${API_KEY:-}"
  upsert EMBEDDING_API_KEY  "${EMBEDDING_API_KEY:-}"
  upsert EMBEDDING_PROVIDER "${EMBEDDING_PROVIDER:-}"
  upsert EMBEDDING_MODEL    "${EMBEDDING_MODEL:-}"
  upsert EMBEDDING_BASE_URL "${EMBEDDING_BASE_URL:-}"
  upsert EMBEDDING_DIMENSION "${EMBEDDING_DIMENSION:-}"
else
  echo "==> .env already exists, leaving it untouched"
fi

echo "==> Warming the Maven dependency cache (offline resolve)"
./mvnw -B -q -DskipTests dependency:go-offline || true

echo "==> Done. Run 'just up' to start the stack, or 'just dev' for the app only."