#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if rg -n "\bvar\s+[A-Za-z_][A-Za-z0-9_]*\s*=" "$ROOT/apps/backend/src/main/java" "$ROOT/apps/relay/src/main/java" "$ROOT/libs/events/src/main/java" --glob "*.java"; then
  echo "Backend Java production code must use explicit local variable types instead of var." >&2
  exit 1
fi

timeout 900s bash -lc "cd '$ROOT' && mvn test"
timeout 900s bash -lc "cd '$ROOT/apps/backend' && mvn spotless:check -DspotlessFiles=src/main/java/com/taskmind/backend/onboarding/interfaces/rest/OnboardingController.java,src/main/java/com/taskmind/backend/auth/infrastructure/e2e/E2eSuperAdminSeeder.java"
timeout 900s bash -lc "cd '$ROOT/apps/relay' && mvn spotless:check -DspotlessFiles=src/main/java/com/taskmind/relay/security/RelaySecurityConfig.java"
if [ -f "$ROOT/apps/frontend/package.json" ]; then
  timeout 180s bash -lc "cd '$ROOT/apps/frontend' && npm run typecheck"
fi
