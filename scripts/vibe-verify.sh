#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

timeout 900s bash -lc "cd '$ROOT/apps/backend' && mvn test"
if [ -f "$ROOT/apps/frontend/package.json" ]; then
  timeout 180s bash -lc "cd '$ROOT/apps/frontend' && npm run typecheck"
fi
