# TaskMind Frontend Feature Skill

Use this skill for Vue, TypeScript, routing, state, API-client, or UI behavior in
`apps/frontend`.

## Minimal context

1. Read `AGENTS.md`.
2. Read `docs/ai/memory.md`.
3. Read `docs/build-kit/phases/M03-frontend-shell.md` or the active feature phase.
4. Read `docs/build-kit/reference/frontend.md`.
5. Search feature APIs, stores, routes, and adjacent tests before opening broad files.

## Discovery commands

```bash
rg --files apps/frontend/src | sed -n '1,160p'
rg -n "<route|store|component|api-field>" apps/frontend/src docs/build-kit/reference/frontend.md
```

## Workflow

- Keep browser traffic routed through Core only.
- Keep API calls in typed feature APIs and shared client helpers.
- Align frontend typings with documented Core contracts.
- Update `apps/backend/openapi.yaml` only when a Core contract changes in the same work.
- Update the frontend changelog for frontend-visible behavior changes.
- Format changed frontend files and run `npm run typecheck`; run `npm run build` for
  runnable UI changes.
- Capture or record skipped browser E2E evidence for UI milestones.

## Closeout

Record exact commands, skipped-check reasons, applicable skills, delegated-agent usage,
and token usage when provider counts are available.
