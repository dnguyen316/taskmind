# Vibe Coding Token Tracking

TaskMind records AI token usage as a lightweight local ledger so we can improve agent
prompts, workflow steps, skills, and milestone planning with measurable statistics. The
ledger is intentionally separate from product telemetry: it describes coding-agent usage,
not end-user behavior.

## Goals

- Capture token usage for every meaningful AI-assisted coding run.
- Compare token spend by model, agent role, workflow phase, skill, and build-kit milestone.
- Keep raw prompts, secrets, source code excerpts, and customer data out of telemetry.
- Make the process usable from any AI provider UI or API by allowing manual entry.

## Ledger location

The default ledger is `.vibe/token-usage.jsonl`. Runtime ledgers are ignored by git, while
`.vibe/.gitkeep` keeps the local telemetry directory discoverable. Set
`VIBE_TOKEN_LEDGER=/path/to/file.jsonl` or pass `--ledger` if you want a shared or archived
ledger outside the repository.

Each line is a JSON object with these fields:

| Field | Purpose |
| --- | --- |
| `provider`, `model` | AI provider and model name used for the event. |
| `agent` | Agent or role, such as `codex`, `explorer`, `worker`, or `claude`. |
| `workflow` | Phase such as `plan`, `implement`, `review`, `verify`, or `skill-authoring`. |
| `skill` | Skill name used during the event, if any. |
| `milestone` | Build-kit milestone advanced by the work, such as `M03` or `M08`. |
| `prompt_tokens`, `completion_tokens`, `cached_input_tokens`, `reasoning_tokens` | Provider-reported token counts. Use zero for unavailable categories. |
| `total_tokens`, `cost_usd` | Roll-up count and optional provider cost. |
| `task`, `source`, `notes` | Short labels to explain where counts came from and what was being improved. |

## Recording usage

Record one event whenever a coding agent or provider reports token usage for a meaningful
run. Prefer provider/API token counts over estimates.

```bash
make vibe-token-record -- \
  --provider openai \
  --model gpt-5.5 \
  --agent codex \
  --workflow implement \
  --skill taskmind-backend-agent \
  --milestone M08 \
  --prompt-tokens 42100 \
  --completion-tokens 7800 \
  --cached-input-tokens 16000 \
  --reasoning-tokens 5200 \
  --cost-usd 1.2345 \
  --source provider-ui \
  --task "Nova planner facade" \
  --notes "Large context load; inspect whether skill preflight can narrow files."
```

The same command can be run directly:

```bash
python3 scripts/vibe-token-usage.py record --provider openai --model gpt-5.5 --prompt-tokens 1000
```

## Reporting statistics

Generate a Markdown report grouped by date:

```bash
make vibe-token-report
```

Group by model, skill, workflow, milestone, or source:

```bash
make vibe-token-report -- --group-by workflow
make vibe-token-report -- --group-by skill --since 2026-06-01 --until 2026-06-30
```

Generate JSON for dashboards or spreadsheets:

```bash
python3 scripts/vibe-token-usage.py report --format json --group-by model
```

## Using statistics to improve vibe coding

Review the report during retro or milestone closeout and look for:

1. **High prompt tokens by workflow.** Improve inspection steps, add narrower `rg`
   commands, or update a skill to avoid broad context loading.
2. **High completion or reasoning tokens by task type.** Split large changes into smaller
   tasks, create clearer acceptance criteria, or move repeatable guidance into a skill.
3. **High spend by skill.** Audit the skill instructions for redundant examples,
   outdated commands, or missing decision points that cause follow-up turns.
4. **High spend by milestone.** Add build-kit notes, targeted tests, or scaffold scripts
   before starting the next similar milestone.
5. **Low token/high value runs.** Capture those patterns in `docs/agent-session-workflow.md`
   or a TaskMind skill so future agents repeat the efficient path.


## Retrospective cadence

Use token reports to improve the agent workflow instead of only archiving usage. When
Codex Cloud or another provider exposes counts:

1. Run `make vibe-token-report -- --group-by workflow` after each large milestone or after
   three substantial AI-assisted sessions, whichever comes first.
2. Run `make vibe-token-report -- --group-by skill` after introducing or materially
   changing local Codex skills.
3. If one workflow phase repeatedly dominates prompt tokens, tighten the discovery steps
   in [`docs/agent-session-workflow.md`](agent-session-workflow.md), update
   [`docs/ai/memory.md`](ai/memory.md), or move repeated guidance into a focused skill.
4. If completion or reasoning tokens dominate, split future work into smaller lifecycle
   notes under `docs/ai/` or narrower task stubs.
5. Record resulting workflow changes in the relevant feature changelog when they affect
   how agents complete backend or frontend work.

Do not invent token counts. If the provider does not expose usage for a session, skip the
record and note that counts were unavailable.

## Hygiene rules

- Do not paste raw prompts, secrets, tokens, keys, customer data, or large code snippets in
  `notes`.
- Record concise labels, not full conversation transcripts.
- Keep local ledgers uncommitted unless the team explicitly exports a sanitized aggregate
  report for review.
- Treat cost as optional because not every provider or UI exposes exact cost per run.
