#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DATA_DIR="${ARBITER_DATA_DIR:-$ROOT_DIR/data}"
API_KEY="${ARBITER_API_KEY:-}"

if [[ -z "$API_KEY" ]]; then
  if command -v uuidgen >/dev/null 2>&1; then
    API_KEY="local-$(uuidgen)-$(uuidgen)"
  else
    API_KEY="local-$(date +%s)-change-this-development-key"
  fi
fi

"$ROOT_DIR/scripts/build.sh"
echo "Arbiter API key for this run:"
echo "$API_KEY"
java -Darbiter.data.dir="$DATA_DIR" -Darbiter.api.key="$API_KEY" -cp "$ROOT_DIR/out/classes" com.arbiter.ArbiterApplication
