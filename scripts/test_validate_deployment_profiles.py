import json
import subprocess
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("validate-deployment-profiles.py")


class DeploymentProfileValidationTest(unittest.TestCase):
    def run_validator(self, environment, variables):
        task = {"containerDefinitions": [{"name": "core", "environment": variables}]}
        with tempfile.NamedTemporaryFile(mode="w", suffix=".json") as fixture:
            json.dump(task, fixture)
            fixture.flush()
            return subprocess.run(
                [str(SCRIPT), environment, fixture.name], capture_output=True, text=True, check=False
            )

    def test_accepts_production_profile_with_disabled_bypass(self):
        result = self.run_validator("production", [{"name": "SPRING_PROFILES_ACTIVE", "value": "prod"}])
        self.assertEqual(0, result.returncode, result.stderr)

    def test_rejects_e2e_in_conflicting_staging_profiles(self):
        result = self.run_validator("staging", [{"name": "SPRING_PROFILES_ACTIVE", "value": "staging,e2e"}])
        self.assertNotEqual(0, result.returncode)
        self.assertIn("activates the e2e profile", result.stderr)

    def test_rejects_enabled_bypass_in_production(self):
        result = self.run_validator(
            "production",
            [
                {"name": "SPRING_PROFILES_ACTIVE", "value": "prod"},
                {"name": "TASKMIND_AUTH_E2E_BYPASS_ENABLED", "value": "true"},
            ],
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("enables TASKMIND_AUTH_E2E_BYPASS_ENABLED", result.stderr)


if __name__ == "__main__":
    unittest.main()
