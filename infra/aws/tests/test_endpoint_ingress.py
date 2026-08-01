"""Static guardrails for interface VPC endpoint security-group ingress."""

import re
import unittest
from pathlib import Path


AWS_ROOT = Path(__file__).parents[1]
NETWORK_MAIN = AWS_ROOT / "network" / "main.tf"
ENVIRONMENT_ROOTS = (
    AWS_ROOT / "envs" / "staging" / "main.tf",
    AWS_ROOT / "envs" / "production" / "main.tf",
)


def resource_block(source: str, resource_type: str, name: str) -> str:
    match = re.search(
        rf'resource\s+"{resource_type}"\s+"{name}"\s+\{{(.*?)\n\}}',
        source,
        re.S,
    )
    if match is None:
        raise AssertionError(f"Missing {resource_type}.{name}")
    return match.group(1)


class EndpointIngressTest(unittest.TestCase):
    def test_network_endpoint_group_has_no_cidr_ingress(self) -> None:
        source = NETWORK_MAIN.read_text(encoding="utf-8")
        endpoint_group = resource_block(source, "aws_security_group", "vpc_endpoints")

        self.assertNotRegex(endpoint_group, r"(?m)^\s*ingress\s*\{")
        self.assertNotIn("var.vpc_cidr", endpoint_group)

    def test_environment_roots_allow_only_service_groups_on_https(self) -> None:
        for path in ENVIRONMENT_ROOTS:
            with self.subTest(environment=path.parent.name):
                source = path.read_text(encoding="utf-8")
                rule = resource_block(
                    source,
                    "aws_vpc_security_group_ingress_rule",
                    "vpc_endpoints_from_service",
                )

                self.assertIn(
                    "for_each = module.security.service_security_group_ids", rule
                )
                self.assertIn(
                    "security_group_id            = module.network.vpc_endpoint_security_group_id",
                    rule,
                )
                self.assertIn("referenced_security_group_id = each.value", rule)
                self.assertRegex(rule, r"from_port\s+=\s+443")
                self.assertRegex(rule, r"to_port\s+=\s+443")
                self.assertRegex(rule, r'ip_protocol\s+=\s+"tcp"')
                self.assertNotIn("var.vpc_cidr", rule)
                self.assertNotIn("0.0.0.0/0", rule)


if __name__ == "__main__":
    unittest.main()
