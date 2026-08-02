#!/usr/bin/env bash
set -euo pipefail

branch="codex/issue-${ISSUE_NUMBER}"
if git ls-remote --exit-code --heads origin "refs/heads/${branch}" >/dev/null 2>&1; then
  echo "Refusing to overwrite existing remote branch ${branch}." >&2
  exit 1
fi

git switch -c "$branch"
git apply "${CODEX_PATCH_PATH:?CODEX_PATCH_PATH must point to the validated patch}"
git diff --check
if [ -z "$(git status --porcelain)" ]; then
  echo 'Patch applied but produced no changes.' >&2
  exit 1
fi

git config user.name 'github-actions[bot]'
git config user.email '41898282+github-actions[bot]@users.noreply.github.com'
git add --all

if git diff --cached --no-renames --name-only -z |
  grep -zEq '^\.github/(workflows|actions)(/|$)'; then
  echo 'Refusing to commit a GitHub workflow or action modification.' >&2
  exit 1
fi

git commit -m "Implement issue #${ISSUE_NUMBER}"
git push origin "HEAD:refs/heads/${branch}"
