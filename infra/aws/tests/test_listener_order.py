#!/usr/bin/env python3
"""Regression check for ALB-listener -> Core ECS service graph ordering."""

from pathlib import Path
import re
import unittest


AWS_ROOT = Path(__file__).resolve().parents[1]


class ListenerOrderTest(unittest.TestCase):
    def test_edge_readiness_selects_the_environment_listener(self) -> None:
        outputs = (AWS_ROOT / "edge" / "outputs.tf").read_text()
        self.assertRegex(
            outputs,
            re.compile(
                r'output\s+"alb_listener_ready_arn"\s*\{.*?'
                r'var\.environment\s*==\s*"local"\s*\?\s*'
                r'aws_lb_listener\.http\.arn\s*:\s*'
                r'aws_lb_listener\.https\[0\]\.arn',
                re.DOTALL,
            ),
        )

    def test_core_service_consumes_listener_readiness(self) -> None:
        compute = (AWS_ROOT / "compute" / "main.tf").read_text()
        self.assertRegex(
            compute,
            re.compile(
                r'resource\s+"aws_ecs_service"\s+"service"\s*\{.*?'
                r'each\.key\s*!=\s*"core"\s*\|\|\s*'
                r'var\.alb_listener_ready_arn\s*!=\s*""',
                re.DOTALL,
            ),
        )

    def test_environment_roots_pass_edge_readiness_to_compute(self) -> None:
        assignment = re.compile(
            r'alb_listener_ready_arn\s*=\s*module\.edge\.alb_listener_ready_arn'
        )
        for environment in ("staging", "production"):
            with self.subTest(environment=environment):
                root = (AWS_ROOT / "envs" / environment / "main.tf").read_text()
                self.assertRegex(root, assignment)


if __name__ == "__main__":
    unittest.main()
