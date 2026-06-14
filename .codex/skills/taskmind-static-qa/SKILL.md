# TaskMind Static QA Skill

Use this skill for review-only sessions, architecture audits, and task-stub generation
where code should not be changed or tests should not be executed.

## Minimal context

1. Read `AGENTS.md`.
2. Read `docs/ai/memory.md`.
3. Read only the active phase or reference docs needed for the review question.
4. Use targeted `rg` searches before opening files.

## Discovery commands

```bash
rg --files -g 'AGENTS.md' -g 'docs/ai/**' -g 'docs/build-kit/**' | sed -n '1,160p'
rg -n "<topic|endpoint|class|workflow>" AGENTS.md docs apps libs
```

## Review output

- Separate static findings from verification evidence.
- Do not claim implementation completion.
- For each actionable issue, provide a `task-stub{title="..."}` with concrete steps.
- List skipped test commands with exact command and reason.
- Do not commit or create a pull request unless an implementation change is made later.
