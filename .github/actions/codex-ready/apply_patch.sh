#!/usr/bin/env bash
set -euo pipefail

branch="codex/issue-${ISSUE_NUMBER}"
git switch -c "$branch"
git apply "${CODEX_PATCH_PATH:-/tmp/codex.patch}"
git diff --check
test -n "$(git status --short)"

if git diff --name-only | grep -Eq '^\.github/workflows(/|$)'; then
  echo 'Refusing to commit a workflow modification.' >&2
  exit 1
fi

git config user.name 'github-actions[bot]'
git config user.email '41898282+github-actions[bot]@users.noreply.github.com'
git add --all
git commit -m "Implement issue #${ISSUE_NUMBER}"
git push origin "HEAD:refs/heads/${branch}"
