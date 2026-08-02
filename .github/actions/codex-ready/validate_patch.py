#!/usr/bin/env python3
import json
import os
import tempfile
from pathlib import Path, PurePosixPath


def parse_git_path(value: str) -> str:
    """Decode one path using Git's core.quotePath C-style quoting rules."""
    value = value.strip()
    if not value.startswith('"'):
        return value
    if len(value) < 2 or not value.endswith('"'):
        raise SystemExit(f"Invalid quoted patch path: {value}")

    decoded = bytearray()
    index = 1
    escapes = {
        "a": 7,
        "b": 8,
        "t": 9,
        "n": 10,
        "v": 11,
        "f": 12,
        "r": 13,
        '"': 34,
        "\\": 92,
    }
    while index < len(value) - 1:
        character = value[index]
        if character != "\\":
            decoded.extend(character.encode("utf-8"))
            index += 1
            continue
        index += 1
        if index >= len(value) - 1:
            raise SystemExit(f"Invalid quoted patch path: {value}")
        character = value[index]
        if character in escapes:
            decoded.append(escapes[character])
            index += 1
        elif character in "01234567":
            end = index + 1
            while end < min(index + 3, len(value) - 1) and value[end] in "01234567":
                end += 1
            decoded.append(int(value[index:end], 8))
            index = end
        else:
            raise SystemExit(f"Invalid escape in quoted patch path: {value}")
    return decoded.decode("utf-8", errors="surrogateescape")


def split_git_paths(value: str) -> list[str]:
    """Split a sequence of quoted or unquoted Git paths without losing spaces."""
    tokens: list[str] = []
    index = 0
    while index < len(value):
        while index < len(value) and value[index].isspace():
            index += 1
        if index == len(value):
            break
        start = index
        if value[index] == '"':
            index += 1
            escaped = False
            while index < len(value):
                character = value[index]
                index += 1
                if character == '"' and not escaped:
                    break
                if character == "\\" and not escaped:
                    escaped = True
                else:
                    escaped = False
            else:
                raise SystemExit(f"Invalid quoted patch path list: {value}")
        else:
            while index < len(value) and not value[index].isspace():
                index += 1
        tokens.append(parse_git_path(value[start:index]))
    return tokens


def collect_patch_paths(patch: str) -> list[str]:
    """Collect every effective old and new path named by a Git patch."""
    paths: list[str] = []
    for line in patch.splitlines():
        if line.startswith("diff --git "):
            header_paths = split_git_paths(line.removeprefix("diff --git "))
            if len(header_paths) != 2:
                raise SystemExit(f"Invalid diff header: {line}")
            for prefix, path in zip(("a/", "b/"), header_paths, strict=True):
                if not path.startswith(prefix):
                    raise SystemExit(f"Invalid diff header: {line}")
                paths.append(path.removeprefix(prefix))
        else:
            for prefix in ("rename from ", "rename to ", "copy from ", "copy to "):
                if line.startswith(prefix):
                    paths.append(parse_git_path(line.removeprefix(prefix)))
                    break
            else:
                if not line.startswith(("--- ", "+++ ")):
                    continue
                raw_path = line[4:].split("\t", 1)[0]
                path = parse_git_path(raw_path)
                if path != "/dev/null":
                    if not path.startswith(("a/", "b/")):
                        raise SystemExit(f"Invalid file header: {line}")
                    paths.append(path[2:])
    return paths


def main() -> None:
    result_path = os.environ.get("CODEX_RESULT_PATH")
    if not result_path:
        raise SystemExit("CODEX_RESULT_PATH is required")
    try:
        raw = Path(result_path).read_text(encoding="utf-8")
    except OSError as error:
        raise SystemExit(f"Unable to read Codex result: {error}") from error
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

    paths = collect_patch_paths(patch)

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
