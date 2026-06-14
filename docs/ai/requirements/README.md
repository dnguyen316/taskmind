# Requirements Phase Template

Use this template when a feature or fix needs clarification before design or code.

## Session header

- Milestone:
- Primary owner: `apps/backend` | `apps/relay` | `apps/ai` | `apps/frontend` | shared lib | docs
- User-visible outcome:
- Canonical phase doc:
- Owning reference doc:

## Requirements checklist

- [ ] State the behavior in one paragraph.
- [ ] List acceptance criteria as concrete checks.
- [ ] Identify affected contracts, events, schemas, or routes.
- [ ] Note security, auth, privacy, or service-boundary constraints.
- [ ] List the smallest docs/files to inspect next.

## Token discipline

Read `docs/ai/memory.md`, the active phase doc, and one owning reference doc before
opening implementation files. Prefer targeted `rg -n` searches over broad file reads.
