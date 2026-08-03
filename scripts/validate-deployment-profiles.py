#!/usr/bin/env python3
"""Reject E2E authentication configuration in remote ECS task definitions."""

import argparse
import json
import re
import sys
from pathlib import Path


BYPASS_KEYS = {
    "TASKMIND_AUTH_E2E_BYPASS_ENABLED",
    "taskmind.auth.e2e-bypass.enabled",
}


def fail(message: str) -> None:
    print(f"Deployment profile validation failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def validate(environment: str, paths: list[Path]) -> None:
    if environment not in {"staging", "production"}:
        fail(f"unsupported deployment environment {environment!r}")

    for path in paths:
        document = json.loads(path.read_text())
        for container in document.get("containerDefinitions", []):
            name = container.get("name", "<unnamed>")
            environment_entries = container.get("environment", [])
            secret_entries = container.get("secrets", [])
            entries = environment_entries + secret_entries
            values = {
                entry.get("name"): str(entry.get("value", entry.get("valueFrom", "")))
                for entry in entries
            }
            profiles = {
                part.strip().lower()
                for part in values.get("SPRING_PROFILES_ACTIVE", "").split(",")
                if part.strip()
            }
            if "e2e" in profiles:
                fail(f"{path}: container {name} activates the e2e profile")
            for key in BYPASS_KEYS:
                if values.get(key, "").lower() == "true":
                    fail(f"{path}: container {name} enables {key}")
                if any(entry.get("name") == key for entry in secret_entries):
                    fail(f"{path}: container {name} sources {key} from a secret")

            serialized = json.dumps(container)
            if re.search(
                r"taskmind\.auth\.e2e-bypass\.(?:enabled|allow-dangerous-local-seed)\s*=\s*true",
                serialized,
                re.I,
            ):
                fail(
                    f"{path}: container {name} enables an E2E bypass property "
                    "in command-line configuration"
                )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("environment")
    parser.add_argument("task_definitions", nargs="+", type=Path)
    args = parser.parse_args()
    validate(args.environment, args.task_definitions)


if __name__ == "__main__":
    main()
