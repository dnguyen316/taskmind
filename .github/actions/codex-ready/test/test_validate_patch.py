import importlib.util
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).parents[1] / "validate_patch.py"
SPEC = importlib.util.spec_from_file_location("validate_patch", MODULE_PATH)
assert SPEC and SPEC.loader
validate_patch = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(validate_patch)


class PatchPathTest(unittest.TestCase):
    def test_collects_rename_source_and_destination(self) -> None:
        patch = """diff --git a/safe.txt b/safe.txt
similarity index 100%
rename from .github/actions/blocked
rename to safe.txt
"""

        self.assertEqual(
            validate_patch.collect_patch_paths(patch),
            ["safe.txt", "safe.txt", ".github/actions/blocked", "safe.txt"],
        )

    def test_collects_delete_path(self) -> None:
        patch = """diff --git a/.github/workflows/ci.yml b/.github/workflows/ci.yml
deleted file mode 100644
--- a/.github/workflows/ci.yml
+++ /dev/null
"""

        paths = validate_patch.collect_patch_paths(patch)

        self.assertIn(".github/workflows/ci.yml", paths)

    def test_decodes_git_quoted_paths(self) -> None:
        patch = 'diff --git "a/name with space" "b/name with space"\n'

        self.assertEqual(
            validate_patch.collect_patch_paths(patch),
            ["name with space", "name with space"],
        )

    def test_decodes_octal_utf8_paths(self) -> None:
        self.assertEqual(validate_patch.parse_git_path('"caf\\303\\251.txt"'), "café.txt")


if __name__ == "__main__":
    unittest.main()
