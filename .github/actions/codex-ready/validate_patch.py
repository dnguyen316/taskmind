#!/usr/bin/env python3
import json
import os
import tempfile
from pathlib import Path, PurePosixPath


def main() -> None:
    result_path = os.environ.get("CODEX_RESULT_PATH")
    raw = Path(result_path).read_text(encoding="utf-8") if result_path else ""
    try:
        result = json.loads(raw)
    except json.JSONDecodeError as error:
        raise SystemExit(f"Codex result is not valid JSON: {error}") from error
    if not isinstance(result, dict):
        raise SystemExit("Codex result must be a JSON object")
    patch = result.get("patch") or ""
    if not isinstance(patch, str):
        raise SystemExit("Codex patch field must be a string")
    if not patch.strip():
        raise SystemExit("Codex returned an empty patch")

    paths: list[str] = []
    for line in patch.splitlines():
        if line.startswith("diff --git a/"):
            parts = line.split(" ")
            if len(parts) != 4 or not parts[3].startswith("b/"):
                raise SystemExit(f"Invalid diff header: {line}")
            paths.extend((parts[2][2:], parts[3][2:]))

    if not paths:
        raise SystemExit("Codex result contains no git diff headers")
    for raw_path in paths:
        path = PurePosixPath(raw_path)
        if path.is_absolute() or ".." in path.parts:
            raise SystemExit(f"Unsafe patch path: {raw_path}")
        if path.parts[:2] in {
            (".github", "workflows"),
            (".github", "actions"),
        }:
            raise SystemExit(f"GitHub automation modifications are forbidden: {raw_path}")

    default_dir = os.environ.get("RUNNER_TEMP") or tempfile.gettempdir()
    patch_path = Path(
        os.environ.get("CODEX_PATCH_PATH") or Path(default_dir) / "codex.patch"
    )
    patch_path.write_text(patch if patch.endswith("\n") else f"{patch}\n", encoding="utf-8")


if __name__ == "__main__":
    main()
