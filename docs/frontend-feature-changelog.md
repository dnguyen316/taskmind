# Frontend Feature Changelog

## 2026-06-06 (M04 Scheduler UI Slice)

### Added

- Added the scheduler frontend API/composable layer for Core-only scheduling preferences, generation, block list/update, and completion calls.
- Replaced the calendar placeholder with live scheduler states for loading, API errors, empty schedules, preference editing, generated block results, missed-block warnings, and reschedule proposal review.
- Added focused scheduler components for the calendar toolbar, preference form, scheduled block list, and reschedule proposal list.

### Frontend-visible change recorded

- `/calendar` now surfaces persisted scheduler-owned blocks from Core, lets users save optimistic-lock protected scheduling preferences, marks missed blocks prominently, supports block completion and time edits, and displays proposals returned by schedule generation.
- The scheduler feature continues to call only Core `/v1/scheduler/**` endpoints; no Relay or Nova direct frontend calls were introduced.

### Verification notes

- E2E status: blocked in this container because no browser binary/Playwright install and no Docker/Postgres runtime are available for a full `/calendar` super-admin bypass flow; use `superadmin@taskmind.local` / `1` / `1` once local services are running.
- Applicable skills: none. Delegated agents: none.

## 2026-06-06 (M03 Auth Session Flow + FE Workflow Closeout)

### Added

- Added this frontend changelog as the FE-visible companion to `docs/backend-feature-changelog.md` so UI sessions have the same closeout traceability as backend sessions.
- Extended `docs/agent-session-workflow.md` with a frontend session closeout sequence that applies the backend best-practice loop to `apps/frontend`: milestone mapping, scoped build-kit docs, changelog entry, contract sync, formatter/typecheck/build ordering, `make vibe-verify`, and browser E2E/skipped-E2E notes.
- Updated `AGENTS.md` and the frontend reference so future frontend agents treat docs/changelog updates as part of the polish loop when behavior changes.
- Added frontend closeout guidance to record applicable Codex skills and agent/delegation usage; this pass used no specialized skill and no delegated sub-agent.

### Frontend-visible change recorded

- M03 auth/session work now aligns the SPA with Core email signup + OTP verification, `/v1/auth/me` profile loading, refresh-token logout, authenticated user display, and current-user ownership for task/project flows.
- The frontend continues to call only Core through typed feature API modules; no Relay or Nova direct calls were introduced.

### Verification notes

- The previous implementation session ran `npm run typecheck`, `npm run build`, and `make vibe-verify` successfully.
- Browser E2E proof remains the required follow-up for M03 UI completion when a browser-capable environment is available.

### Known follow-up

- Add an automated browser E2E harness or document the manual super-admin bypass flow once local app orchestration is available in the agent environment.
- Continue recording frontend-visible behavior changes in this changelog, newest entry first.
