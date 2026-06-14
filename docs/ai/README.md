# TaskMind AI Agent Workflow

Use this directory as the compact, searchable control plane for Codex Cloud sessions.
It turns the broader build-kit into small lifecycle artifacts so agents can preserve
context, avoid re-reading unrelated docs, and leave repeatable evidence for future runs.

## Recommended lifecycle

1. Capture scope in [`requirements/`](requirements/README.md).
2. Record architecture or contract choices in [`design/`](design/README.md).
3. Break the work into bounded steps in [`planning/`](planning/README.md).
4. Track implementation notes in [`implementation/`](implementation/README.md).
5. Record verification evidence in [`testing/`](testing/README.md).

Tiny bug fixes may use only `AGENTS.md` and `docs/agent-session-workflow.md`, but larger
feature slices should leave the smallest useful artifact in this directory.

## Token-saving rule

Before opening broad build-kit docs, read [`memory.md`](memory.md), then open only the
active milestone phase and one owning reference document.
