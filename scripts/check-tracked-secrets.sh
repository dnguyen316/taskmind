#!/usr/bin/env bash
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
prohibited=()

while IFS= read -r -d '' path; do
  basename="${path##*/}"

  # Explicitly sanitized templates are safe to keep under version control.
  if [[ "$basename" == ".env.example" || "$basename" == *.tfvars.example ]]; then
    continue
  fi

  if [[ "$basename" == ".env" || "$basename" == .env.* \
    || "$basename" == *.tfvars || "$basename" == *.tfvars.json \
    || "$basename" == *.tfstate || "$basename" == *.tfstate.* \
    || "$basename" == tfplan* \
    || "$basename" == credentials.csv \
    || "$basename" == aws-credentials* || "$basename" == aws_credentials* \
    || "$basename" == *.pem || "$basename" == *.key \
    || "$basename" == *.p12 || "$basename" == *.pfx \
    || "$basename" == *.jks || "$basename" == *.keystore \
    || "$basename" == *-task-definition.json \
    || "$path" == .terraform/* || "$path" == */.terraform/* \
    || "$path" == .aws/credentials || "$path" == */.aws/credentials ]]; then
    prohibited+=("$path")
  fi
done < <(git -C "$ROOT" ls-files -z)

if ((${#prohibited[@]} > 0)); then
  echo "Tracked files match prohibited secret, credential, state, or generated-artifact patterns:" >&2
  printf '  - %s\n' "${prohibited[@]}" >&2
  echo "Remove them from Git and rotate any exposed credentials before continuing." >&2
  exit 1
fi

echo "Tracked filename safety check passed."
