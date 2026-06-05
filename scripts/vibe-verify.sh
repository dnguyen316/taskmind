#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../apps/backend"
timeout 900s mvn test
