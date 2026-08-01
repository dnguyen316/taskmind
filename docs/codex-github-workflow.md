# Codex GitHub label workflow

TaskMind uses one mutually exclusive `codex-*` status label to communicate an issue's
place in the Codex work queue. `human-review` is an attention flag rather than a status,
so it may accompany any status where a person must act. Label names, colors, and short
GitHub descriptions are owned by [`.github/labels.yml`](../.github/labels.yml).

## Label meanings and ownership

| Label | Meaning | Who may apply or remove it |
| --- | --- | --- |
| `codex-planned` | The work is scoped, but a human has not approved it to enter the executable queue. | Human only. |
| `codex-ready` | A human has approved the scope and supplied enough acceptance and verification detail for Codex to start. | Human only when approving work; status automation may restore it after an automated retry or revision handoff. |
| `codex-in-progress` | Codex has claimed the issue and is actively implementing it. | Status automation when a Codex run starts. A human may correct stale state. |
| `codex-review` | Codex has finished its implementation and checks, and the result awaits validation. | Status automation after a successful Codex run. A human may correct stale state. |
| `codex-blocked` | Work cannot proceed until a dependency, missing input, or decision is resolved. | Status automation may apply it when a run reports a blocker; a human may apply or clear it. |
| `human-review` | A person must make a decision, review a result, or provide input. It does not replace the current `codex-*` status. | Automation may request attention; only a human clears it after addressing the request. |
| `codex-done` | A human has accepted the result and no further Codex work is required. | Human only. |

## Permitted transitions

Only one `codex-*` status should be present after a transition. The actor replaces the
source status with the destination status atomically where the integration permits it.

| From | To | Actor and reason |
| --- | --- | --- |
| none | `codex-planned` | Human records proposed work. |
| `codex-planned` | `codex-ready` | Human approves a complete, actionable issue. |
| `codex-ready` | `codex-in-progress` | Status automation claims the issue when a Codex run starts. |
| `codex-in-progress` | `codex-review` | Status automation reports a completed run ready for validation. |
| `codex-in-progress` | `codex-blocked` | Status automation or a human records that progress cannot continue. |
| `codex-blocked` | `codex-ready` | Human confirms the blocker is resolved and requeues the work. |
| `codex-review` | `codex-ready` | Human requests changes and requeues the issue with updated direction. |
| `codex-review` | `codex-done` | Human accepts the work. This acceptance is never automated. |
| `codex-done` | `codex-planned` | Human reopens the lifecycle because new scope is required. |

When automation needs human input, it adds `human-review` without changing the status
unless work is actually blocked; in that case it also transitions `codex-in-progress` to
`codex-blocked`. A human removes `human-review` only after resolving the request.

No other status transition is permitted. In particular, automation must not promote
planned work to ready, accept review as done, or reopen done work. Humans may repair an
incorrect or stale label, but should otherwise follow the table so queue history remains
meaningful.

## Manifest synchronization

The **Sync managed labels** workflow is manual (`workflow_dispatch`). It reads the
checked-in manifest and creates missing labels or updates the color and description of
existing labels. It neither deletes labels nor adds labels to issues, so it performs no
lifecycle transition. The workflow has only `contents: read` (to read the manifest) and
`issues: write` (to manage label metadata); it has no repository-content write access.

To change the managed set, review and merge a manifest edit, then manually dispatch the
workflow from GitHub Actions. Labels absent from the manifest are deliberately untouched.
