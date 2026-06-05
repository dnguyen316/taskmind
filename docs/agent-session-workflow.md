# Agent Session Workflow

Use this lightweight workflow to keep backend implementation sessions traceable without
turning every pass into a broad repository audit. It is backend-aware but
service-boundary neutral: apply it to Core (`apps/backend`), Relay (`apps/relay`), Nova
(`apps/ai`), and shared backend libraries while keeping each change inside the service or
library that owns the concern.

## Backend session closeout sequence

Before closing a backend-focused session, complete this sequence and call out any item
that does not apply:

1. **Identify the touched milestone.** Check the milestone map in
   [`docs/build-kit/01-build-order.md`](build-kit/01-build-order.md) and record which
   milestone the work advances. If the change spans multiple milestones, name the primary
   milestone first and list secondary milestones separately.
2. **Update build-kit docs only when needed.** Update the relevant phase or reference doc
   only when behavior, contracts, or architecture changed. Do not churn roadmap docs for
   purely internal refactors, formatting-only edits, or tests that do not alter expected
   behavior.
3. **Add the backend changelog entry.** Add a dated entry to
   [`docs/backend-feature-changelog.md`](backend-feature-changelog.md) summarizing the
   backend-visible change, the milestone it advances, and any notable follow-up work.
4. **Keep Core OpenAPI synchronized.** When Core request or response contracts change,
   update `apps/backend/openapi.yaml` in the same change set. Relay and Nova internal
   contract changes should be reflected in their owning build-kit reference docs instead.
5. **Run checks in the right order.** When implementation mode allows tests, run the
   smallest targeted backend checks for the touched service or library first, then run the
   repository quality gate with `make vibe-verify` before claiming the session is done.
6. **Record skipped checks.** If any targeted check or `make vibe-verify` is skipped,
   record the exact command that was skipped and the reason, such as documentation-only
   work, unavailable dependencies, time constraints, or an environment limitation.

## Token-saving inspection workflow

Use this inspection pattern before opening large files or scanning broad directories:

1. **Start with file discovery.** Use `rg --files` with path or glob filters instead of
   broad recursive listings.
2. **Read only the active guidance.** Read `AGENTS.md`, the active build-kit phase doc,
   and the specific reference doc that owns the behavior or contract under review. Avoid
   loading unrelated phases or the whole reference tree.
3. **Search identifiers before opening files.** Use `rg -n` for endpoint paths, class
   names, DTO names, migration names, event names, configuration keys, and test names
   before opening implementation files.
4. **Inspect adjacent tests first.** Open tests next to the changed code before expanding
   to broader package or service scans.
5. **Summarize before loading more.** Write down the current findings, assumptions, and
   remaining unknowns before opening additional files. Load more context only when the
   summary shows a concrete gap.

This workflow is intentionally small: it should reduce context waste while preserving the
required closeout evidence for backend sessions across Core, Relay, and Nova.
