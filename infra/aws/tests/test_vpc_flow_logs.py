import re
from pathlib import Path


AWS_ROOT = Path(__file__).resolve().parents[1]
NETWORK_MAIN = (AWS_ROOT / "network" / "main.tf").read_text()
NETWORK_VARIABLES = (AWS_ROOT / "network" / "variables.tf").read_text()


def test_flow_logs_are_encrypted_and_capture_security_fields():
    assert 'resource "aws_cloudwatch_log_group" "vpc_flow_logs"' in NETWORK_MAIN
    assert "kms_key_id        = local.flow_log_kms_key_id" in NETWORK_MAIN
    assert 'resource "aws_flow_log" "this"' in NETWORK_MAIN
    assert 'traffic_type             = "ALL"' in NETWORK_MAIN

    for field in (
        "interface-id",
        "srcaddr",
        "dstaddr",
        "srcport",
        "dstport",
        "protocol",
        "action",
        "flow-direction",
        "tcp-flags",
        "reject-reason",
    ):
        assert f"$${{{field}}}" in NETWORK_MAIN


def test_delivery_role_and_rejected_traffic_alarm_are_scoped():
    assert 'identifiers = ["vpc-flow-logs.amazonaws.com"]' in NETWORK_MAIN
    assert 'resources = ["${aws_cloudwatch_log_group.vpc_flow_logs.arn}:*"]' in NETWORK_MAIN
    assert 'action = REJECT' in NETWORK_MAIN
    assert 'resource "aws_cloudwatch_metric_alarm" "unusual_rejected_traffic"' in NETWORK_MAIN
    assert "alarm_actions       = var.alarm_topic_arns" in NETWORK_MAIN


def test_retention_and_kms_are_configurable_with_longer_production_retention():
    assert 'variable "flow_log_retention_days"' in NETWORK_VARIABLES
    assert 'variable "flow_log_kms_key_id"' in NETWORK_VARIABLES

    production = (AWS_ROOT / "envs" / "production" / "main.tf").read_text()
    staging = (AWS_ROOT / "envs" / "staging" / "main.tf").read_text()
    assert "flow_log_retention_days = 365" in production
    assert "flow_log_retention_days = 30" in staging
    assert re.search(r"flow_log_kms_key_id\s*=\s*var\.cloudwatch_logs_kms_key_id", production)


def test_optional_variable_validation_does_not_evaluate_functions_with_null():
    assert (
        'var.flow_log_kms_key_id == null ? true : trimspace(var.flow_log_kms_key_id) != ""'
        in NETWORK_VARIABLES
    )
    assert "var.flow_log_retention_days == null ? true : contains(" in NETWORK_VARIABLES
