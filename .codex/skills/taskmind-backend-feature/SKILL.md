# TaskMind Backend Feature Skill

Use this skill for backend behavior in `apps/backend`, `apps/relay`, `apps/ai`, or shared
Java libraries.

## Minimal context

1. Read `AGENTS.md`.
2. Read `docs/ai/memory.md`.
3. Read the active `docs/build-kit/phases/M*.md` file.
4. Read one owning reference doc from `docs/build-kit/reference/`.
5. Search adjacent tests before opening implementation packages.

## Discovery commands

```bash
rg --files apps/backend apps/relay apps/ai libs | sed -n '1,160p'
rg -n "<endpoint|class|dto|event|config-key>" apps libs docs/build-kit/reference
```

## Workflow

- Write or update failing Java tests first for behavior changes.
- Keep changes inside the owning service or shared library.
- Add Flyway migrations only by appending the next integer migration.
- Update `apps/backend/openapi.yaml` when Core public contracts change.
- Update the backend changelog for backend-visible behavior changes.
- Format changed Java files from the owning service with Spotless when Java changed.
- Run targeted Maven tests before `make vibe-verify`.

## Closeout

Record exact commands, skipped-check reasons, applicable skills, delegated-agent usage,
and token usage when provider counts are available.
