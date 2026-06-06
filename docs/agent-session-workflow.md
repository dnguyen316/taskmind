# Agent Session Workflow

Use this lightweight workflow to keep implementation sessions traceable without
turning every pass into a broad repository audit. It started as the backend closeout
standard, and the same habits now apply to frontend sessions: identify the milestone,
update only the owning docs, record the changelog entry, run focused checks before the
full quality gate, and document anything skipped.

For backend work, keep changes inside Core (`apps/backend`), Relay (`apps/relay`), Nova
(`apps/ai`), or the shared backend library that owns the concern. For frontend work, keep
changes inside `apps/frontend`, call Core only through typed feature APIs, and avoid
direct Relay or Nova calls.

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
7. **Record AI token usage when available.** When the coding agent, provider UI, or API
   reports token counts, append one sanitized event with
   `make vibe-token-record -- <fields>` so milestone retros can compare usage by model,
   workflow phase, skill, and agent role. See
   [`docs/vibe-token-tracking.md`](vibe-token-tracking.md) for fields and examples.

## Frontend session closeout sequence

Apply the backend best-practice loop above to `apps/frontend` with frontend-specific
ownership and verification:

1. **Identify the touched milestone.** Most shell/auth/task/project UI work advances
   **M03**. Later feature slices should map to their owning milestones (for example M04
   scheduler, M08 AI, M10 integrations, M11 notifications, or M12 reports/dashboard).
2. **Keep docs scoped to behavior.** Update [`docs/build-kit/reference/frontend.md`](build-kit/reference/frontend.md)
   or the active phase doc only when UX behavior, route guards, API contracts, state
   ownership, or verification expectations change. Avoid doc churn for pure styling or
   formatting.
3. **Add the frontend changelog entry.** Add a dated entry to
   [`docs/frontend-feature-changelog.md`](frontend-feature-changelog.md) for
   frontend-visible behavior, workflow changes, known gaps, and follow-up work.
4. **Keep Core contracts aligned.** If a frontend change depends on a new or changed Core
   request/response shape, update `apps/backend/openapi.yaml` and the owning frontend API
   typings together. If the frontend only consumes an already-documented Core contract,
   cite that contract in the changelog instead of editing OpenAPI.
5. **Run checks in the right order.** Format changed frontend files with
   `npm run format -- <changed-fe-files>`, run `npm run typecheck`, run `npm run build`
   for runnable UI changes, then run `make vibe-verify` before claiming the session is
   complete.
6. **Record UI proof or skipped E2E.** For UI milestones, record the browser E2E flow on
   `localhost:5173` with the super-admin bypass. If a browser is unavailable, record the
   exact skipped E2E flow and environment limitation in the final response and changelog.
7. **Record skills and agent role.** Note which Codex skills, if any, were used and whether
   the work was completed locally or with delegated agents. If no skill or sub-agent was
   applicable, say so in the changelog/final response instead of inventing one.
8. **Record AI token usage when available.** Use the same token-recording guidance as the
   backend workflow when token counts are available.

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
6. **Use the token report as feedback.** If `make vibe-token-report -- --group-by workflow`
   shows recurring high prompt-token usage for a workflow step, tighten that step with
   more specific discovery commands or move repeated guidance into a skill.

This workflow is intentionally small: it should reduce context waste while preserving the
required closeout evidence for backend sessions across Core, Relay, and Nova.
