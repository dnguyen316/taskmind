"""Static guardrails for the allowlisted TaskMind security-group paths."""

import re
import unittest
from pathlib import Path


SECURITY_MAIN = Path(__file__).parents[1] / "security" / "main.tf"


class SecurityPathTest(unittest.TestCase):
    def setUp(self) -> None:
        self.source = SECURITY_MAIN.read_text(encoding="utf-8")

    def test_ingress_rule_inventory_is_allowlisted(self) -> None:
        resources = set(
            re.findall(
                r'resource\s+"aws_vpc_security_group_ingress_rule"\s+"([^"]+)"',
                self.source,
            )
        )
        self.assertEqual(
            resources,
            {
                "core_to_relay",
                "core_to_nova",
                "nova_to_core",
                "nova_to_relay",
                "rds_from_service",
                "redis_from_service",
                "opensearch_from_relay",
                "opensearch_from_core",
            },
        )

    def test_security_groups_do_not_hide_inline_ingress(self) -> None:
        group_blocks = self.source.split(
            'resource "aws_vpc_security_group_ingress_rule"', 1
        )[0]
        self.assertNotRegex(group_blocks, r"(?m)^\s+ingress\s*{")
        self.assertNotIn('resource "aws_security_group_rule"', self.source)

    def test_data_rules_authorize_only_named_services(self) -> None:
        for resource in ("rds_from_service", "redis_from_service"):
            start = self.source.index(
                f'resource "aws_vpc_security_group_ingress_rule" "{resource}"'
            )
            end = self.source.index("\n}\n", start)
            block = self.source[start:end]
            callers = set(
                re.findall(r"^\s+(core|relay|nova)\s+=\s+aws_security_group", block, re.M)
            )
            self.assertEqual(callers, {"core", "relay", "nova"})

    def test_core_and_relay_have_no_unrestricted_all_protocol_egress(self) -> None:
        for service in ("core", "relay"):
            group = re.search(
                rf'resource\s+"aws_security_group"\s+"{service}"\s+\{{(.*?)\n\}}',
                self.source,
                re.S,
            )
            self.assertIsNotNone(group)
            self.assertNotRegex(
                group.group(1),
                r'protocol\s*=\s*"-1".*cidr_blocks\s*=\s*\["0\.0\.0\.0/0"\]',
            )

        all_egress_blocks = re.findall(
            r'resource\s+"aws_vpc_security_group_egress_rule"\s+"([^"]+)"\s+\{(.*?)\n\}',
            self.source,
            re.S,
        )
        for name, block in all_egress_blocks:
            if 'cidr_ipv4         = "0.0.0.0/0"' in block:
                self.assertIn(name, {"core_external_integrations", "nova_external_ai"})
                self.assertRegex(block, r'ip_protocol\s*=\s*"tcp"')
                self.assertRegex(block, r'from_port\s*=\s*443')
                self.assertRegex(block, r'to_port\s*=\s*443')


if __name__ == "__main__":
    unittest.main()
