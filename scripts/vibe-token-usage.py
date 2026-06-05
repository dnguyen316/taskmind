#!/usr/bin/env python3
"""Record and report AI token usage for TaskMind vibe-coding sessions.

The ledger is JSON Lines so agents can append one event per AI run without needing a
service, database, or network access. By default it writes to .vibe/token-usage.jsonl,
which is intentionally ignored by git.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import uuid
from collections import defaultdict
from dataclasses import dataclass
from datetime import date, datetime, timezone
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any, Iterable

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_LEDGER = ROOT / ".vibe" / "token-usage.jsonl"
ISO_FORMAT_HINT = "Use ISO-8601, for example 2026-06-05T14:30:00Z."
TOKEN_FIELDS = ("prompt_tokens", "completion_tokens", "cached_input_tokens", "reasoning_tokens")


@dataclass(frozen=True)
class UsageEvent:
    event_id: str
    recorded_at: str
    date: str
    provider: str
    model: str
    agent: str | None
    workflow: str | None
    skill: str | None
    milestone: str | None
    task: str | None
    source: str | None
    prompt_tokens: int
    completion_tokens: int
    cached_input_tokens: int
    reasoning_tokens: int
    total_tokens: int
    cost_usd: str | None
    started_at: str | None
    ended_at: str | None
    notes: str | None

    def to_json(self) -> str:
        return json.dumps(self.__dict__, sort_keys=True, separators=(",", ":"))


def parse_iso_timestamp(value: str | None, field_name: str) -> str | None:
    if value is None:
        return None
    normalized = value.strip()
    if not normalized:
        return None
    candidate = normalized[:-1] + "+00:00" if normalized.endswith("Z") else normalized
    try:
        datetime.fromisoformat(candidate)
    except ValueError as exc:
        raise argparse.ArgumentTypeError(f"Invalid {field_name}: {value!r}. {ISO_FORMAT_HINT}") from exc
    return normalized


def parse_non_negative_int(value: str) -> int:
    try:
        parsed = int(value)
    except ValueError as exc:
        raise argparse.ArgumentTypeError(f"Expected a non-negative integer, got {value!r}") from exc
    if parsed < 0:
        raise argparse.ArgumentTypeError(f"Expected a non-negative integer, got {value!r}")
    return parsed


def parse_cost(value: str | None) -> str | None:
    if value is None or value == "":
        return None
    try:
        parsed = Decimal(value)
    except InvalidOperation as exc:
        raise argparse.ArgumentTypeError(f"Invalid USD cost: {value!r}") from exc
    if parsed < 0:
        raise argparse.ArgumentTypeError("USD cost must be non-negative")
    return format(parsed, "f")


def event_date(started_at: str | None) -> str:
    if started_at:
        candidate = started_at[:-1] + "+00:00" if started_at.endswith("Z") else started_at
        return datetime.fromisoformat(candidate).date().isoformat()
    return date.today().isoformat()


def now_utc() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def ledger_path(args: argparse.Namespace) -> Path:
    return Path(args.ledger).expanduser().resolve()


def record(args: argparse.Namespace) -> int:
    started_at = parse_iso_timestamp(args.started_at, "started-at")
    ended_at = parse_iso_timestamp(args.ended_at, "ended-at")
    cost_usd = parse_cost(args.cost_usd)
    total_tokens = args.prompt_tokens + args.completion_tokens + args.cached_input_tokens + args.reasoning_tokens
    if total_tokens == 0:
        print("Refusing to record an event with zero tokens. Supply at least one token count.", file=sys.stderr)
        return 2

    event = UsageEvent(
        event_id=str(uuid.uuid4()),
        recorded_at=now_utc(),
        date=event_date(started_at),
        provider=args.provider,
        model=args.model,
        agent=args.agent,
        workflow=args.workflow,
        skill=args.skill,
        milestone=args.milestone,
        task=args.task,
        source=args.source,
        prompt_tokens=args.prompt_tokens,
        completion_tokens=args.completion_tokens,
        cached_input_tokens=args.cached_input_tokens,
        reasoning_tokens=args.reasoning_tokens,
        total_tokens=total_tokens,
        cost_usd=cost_usd,
        started_at=started_at,
        ended_at=ended_at,
        notes=args.notes,
    )

    path = ledger_path(args)
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as ledger:
        ledger.write(event.to_json() + "\n")
    print(f"Recorded {total_tokens} tokens in {path}")
    return 0


def read_events(path: Path) -> list[dict[str, Any]]:
    if not path.exists():
        return []
    events: list[dict[str, Any]] = []
    with path.open("r", encoding="utf-8") as ledger:
        for line_number, line in enumerate(ledger, start=1):
            stripped = line.strip()
            if not stripped:
                continue
            try:
                event = json.loads(stripped)
            except json.JSONDecodeError as exc:
                raise SystemExit(f"Invalid JSON in {path} line {line_number}: {exc}") from exc
            events.append(event)
    return events


def matches_filters(event: dict[str, Any], args: argparse.Namespace) -> bool:
    for field in ("provider", "model", "agent", "workflow", "skill", "milestone"):
        expected = getattr(args, field, None)
        if expected and event.get(field) != expected:
            return False
    if args.since and event.get("date", "") < args.since:
        return False
    if args.until and event.get("date", "") > args.until:
        return False
    return True


def add_totals(target: dict[str, Any], event: dict[str, Any]) -> None:
    target["events"] += 1
    for field in TOKEN_FIELDS:
        target[field] += int(event.get(field) or 0)
    target["total_tokens"] += int(event.get("total_tokens") or 0)
    if event.get("cost_usd") not in (None, ""):
        target["cost_usd"] += Decimal(str(event["cost_usd"]))


def empty_totals() -> dict[str, Any]:
    return {
        "events": 0,
        "prompt_tokens": 0,
        "completion_tokens": 0,
        "cached_input_tokens": 0,
        "reasoning_tokens": 0,
        "total_tokens": 0,
        "cost_usd": Decimal("0"),
    }


def report(args: argparse.Namespace) -> int:
    path = ledger_path(args)
    events = [event for event in read_events(path) if matches_filters(event, args)]
    if not events:
        print(f"No token usage events found in {path}")
        return 0

    totals = empty_totals()
    groups: dict[str, dict[str, Any]] = defaultdict(empty_totals)
    for event in events:
        add_totals(totals, event)
        group_value = event.get(args.group_by) or "(unset)"
        add_totals(groups[str(group_value)], event)

    if args.format == "json":
        payload = {
            "ledger": str(path),
            "filters": active_filters(args),
            "totals": stringify_decimal(totals),
            "group_by": args.group_by,
            "groups": {key: stringify_decimal(value) for key, value in sorted(groups.items())},
        }
        print(json.dumps(payload, indent=2, sort_keys=True))
        return 0

    print(f"# Vibe token usage report")
    print(f"Ledger: `{path}`")
    print(f"Filters: {active_filters(args) or 'none'}")
    print()
    print("## Totals")
    print_totals(totals)
    print()
    print(f"## By {args.group_by}")
    print("| Group | Events | Prompt | Completion | Cached input | Reasoning | Total | Cost USD |")
    print("|---|---:|---:|---:|---:|---:|---:|---:|")
    for key, value in sorted(groups.items(), key=lambda item: item[1]["total_tokens"], reverse=True):
        print(
            f"| {key} | {value['events']} | {value['prompt_tokens']} | {value['completion_tokens']} | "
            f"{value['cached_input_tokens']} | {value['reasoning_tokens']} | {value['total_tokens']} | "
            f"{format_decimal(value['cost_usd'])} |"
        )
    return 0


def active_filters(args: argparse.Namespace) -> dict[str, str]:
    fields = ("provider", "model", "agent", "workflow", "skill", "milestone", "since", "until")
    return {field: getattr(args, field) for field in fields if getattr(args, field, None)}


def stringify_decimal(value: dict[str, Any]) -> dict[str, Any]:
    output = dict(value)
    output["cost_usd"] = format_decimal(output["cost_usd"])
    return output


def format_decimal(value: Decimal) -> str:
    return format(value.quantize(Decimal("0.000001")), "f")


def print_totals(totals: dict[str, Any]) -> None:
    print(f"- Events: {totals['events']}")
    print(f"- Prompt tokens: {totals['prompt_tokens']}")
    print(f"- Completion tokens: {totals['completion_tokens']}")
    print(f"- Cached input tokens: {totals['cached_input_tokens']}")
    print(f"- Reasoning tokens: {totals['reasoning_tokens']}")
    print(f"- Total tokens: {totals['total_tokens']}")
    print(f"- Cost USD: {format_decimal(totals['cost_usd'])}")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Track AI token usage for TaskMind vibe-coding sessions.")
    parser.add_argument(
        "--ledger",
        default=os.environ.get("VIBE_TOKEN_LEDGER", str(DEFAULT_LEDGER)),
        help="JSONL ledger path. Defaults to .vibe/token-usage.jsonl or VIBE_TOKEN_LEDGER.",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    record_parser = subparsers.add_parser("record", help="Append one token usage event to the ledger.")
    record_parser.add_argument("--provider", required=True, help="AI provider, for example openai, anthropic, or local.")
    record_parser.add_argument("--model", required=True, help="Model name reported by the AI provider or coding agent.")
    record_parser.add_argument("--agent", help="Agent name or role, for example codex, explorer, worker, or claude.")
    record_parser.add_argument("--workflow", help="Workflow phase, for example plan, implement, review, verify, or skill-authoring.")
    record_parser.add_argument("--skill", help="Skill used during the run, if any.")
    record_parser.add_argument("--milestone", help="Build-kit milestone advanced by the run, for example M03 or M08.")
    record_parser.add_argument("--task", help="Short task label or ticket key.")
    record_parser.add_argument("--source", help="Where the counts came from, for example api-response, provider-ui, or manual.")
    record_parser.add_argument("--prompt-tokens", type=parse_non_negative_int, default=0)
    record_parser.add_argument("--completion-tokens", type=parse_non_negative_int, default=0)
    record_parser.add_argument("--cached-input-tokens", type=parse_non_negative_int, default=0)
    record_parser.add_argument("--reasoning-tokens", type=parse_non_negative_int, default=0)
    record_parser.add_argument("--cost-usd", help="Optional provider cost in USD for this event.")
    record_parser.add_argument("--started-at", help=ISO_FORMAT_HINT)
    record_parser.add_argument("--ended-at", help=ISO_FORMAT_HINT)
    record_parser.add_argument("--notes", help="Free-form note; avoid secrets and proprietary prompt text.")
    record_parser.set_defaults(func=record)

    report_parser = subparsers.add_parser("report", help="Summarize recorded token usage.")
    report_parser.add_argument(
        "--group-by",
        choices=("date", "provider", "model", "agent", "workflow", "skill", "milestone", "source"),
        default="date",
    )
    report_parser.add_argument("--format", choices=("markdown", "json"), default="markdown")
    report_parser.add_argument("--provider")
    report_parser.add_argument("--model")
    report_parser.add_argument("--agent")
    report_parser.add_argument("--workflow")
    report_parser.add_argument("--skill")
    report_parser.add_argument("--milestone")
    report_parser.add_argument("--since", help="Inclusive YYYY-MM-DD lower bound.")
    report_parser.add_argument("--until", help="Inclusive YYYY-MM-DD upper bound.")
    report_parser.set_defaults(func=report)
    return parser


def main(argv: Iterable[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
