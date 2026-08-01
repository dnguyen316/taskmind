#!/usr/bin/env python3
import json
import os
from pathlib import Path, PurePosixPath


def main() -> None:
    result = json.loads(os.environ["CODEX_RESULT"])
    patch = result.get("patch", "")
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
        if path.parts[:2] == (".github", "workflows"):
            raise SystemExit(f"Workflow modifications are forbidden: {raw_path}")

    patch_path = Path(os.environ.get("CODEX_PATCH_PATH", "/tmp/codex.patch"))
    patch_path.write_text(patch if patch.endswith("\n") else f"{patch}\n", encoding="utf-8")


if __name__ == "__main__":
    main()
