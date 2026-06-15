# Frontend Feature Changelog

## 2026-06-15 (M03 Task Detail Direct Lookup)

### Changed

- Replaced task detail loading via broad list search with a direct Core `GET /v1/tasks/{id}` request adapted through the existing task response mapper.
- Simplified the task detail composable lookup call so it no longer passes list pagination or user scoping parameters for detail retrieval.

### Frontend-visible change recorded

- Task detail pages now load the requested task directly from Core instead of fetching a larger task list and searching client-side.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Full gate verification: `make vibe-verify`.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused API lookup change. Manual verification should open an authenticated task detail route and confirm the page loads via `GET /v1/tasks/{id}`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-15 (M03 Token Refresh Session Sync)

### Changed

- Added a lightweight token-refresh auth-session event so the shared API client can notify the auth layer without importing Pinia directly.
- Re-applied the stored auth session after successful refresh-token rotation so Pinia updates the access token expiry while preserving the loaded current user.

### Frontend-visible change recorded

- When a protected API call receives `401`, the client refreshes with the stored refresh token, retries the failed request, and keeps the in-memory auth session expiry aligned with local token storage after the retry succeeds.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Full gate verification: `make vibe-verify`.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused auth-session state change. Manual verification should seed an expired access token and valid refresh token, confirm the protected request retries successfully, and confirm `authStore.session.expiresAt` updates without clearing `currentUser`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-14

- Refactored the Projects dashboard into the shared app shell, removed the user-facing owner-id field from project creation, and aligned project creation/list styling with TaskMind surface-card layout patterns.

# Frontend Feature Changelog

## 2026-06-14 (M03 Silent Auth Initialization)

### Changed

- Added a silent profile-refresh mode for auth initialization so expired stored sessions are cleared without surfacing a login form error.
- Kept login and signup verification profile fetches on the default non-silent path so user-submitted authentication failures still populate form errors.

### Frontend-visible change recorded

- Loading the SPA with an expired or invalid stored token now returns the user to unauthenticated routing without showing a stale session error in auth forms.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused store behavior change.
- Applicable skills: none. Delegated agents: none.

## 2026-06-14 (M08 Weekly Review Current User Request)

### Changed

- Removed the weekly review user UUID field so the panel generates reviews for the authenticated session context.
- Updated the weekly review composable to post an empty request body to Core `/v1/review/weekly/generate` while preserving the typed response `userId` returned by the backend.

### Frontend-visible change recorded

- Users can generate a weekly review without manually entering a user UUID; Core remains the only frontend API target for this flow.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Full gate verification: `make vibe-verify`.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused UI contract adjustment.
- Applicable skills: none. Delegated agents: none.

## 2026-06-14 (M08 Capture Acceptance Path)

### Added

- Added per-draft Accept and Reject controls to the AI capture panel.
- Extended the capture composable with typed calls for Core-only `/v1/ai/capture/accept` and `/v1/ai/capture/reject` requests.
- Added typed capture accept/reject request and response contracts for the frontend AI feature.

### Frontend-visible change recorded

- Users can now promote AI-captured drafts into tasks or reject unwanted drafts directly from the capture panel without calling Nova directly from the browser.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Browser E2E outcome: not run in this container; no browser automation was requested for this slice.
- Applicable skills: none. Delegated agents: none.

## 2026-06-07 (M06 Search & Storage UI Slice)

### Added

- Added typed Core-only frontend wrappers for task attachment upload, list, download, delete, and activity search.
- Added OpenAPI-aligned task attachment, media-kind, and activity-search document types for the M06 Core contracts.
- Added a task detail attachment panel for uploading files with a media kind, listing metadata, downloading through Core, and deleting task-scoped attachments.
- Added an activity-search frontend feature with a protected `/activity` route, typed composable, search panel, and sidebar entry that calls only Core `/v1/activity/search`.

### Frontend-visible change recorded

- Task detail pages now surface task-scoped attachment management through Core `/v1/tasks/{taskId}/attachments/**` endpoints.
- `/activity` now provides an authenticated activity search UI backed by Core's M06 search facade; the frontend does not call Relay, Nova, S3, or OpenSearch directly.

### Verification notes

- Exact verification commands for this M06 frontend slice: `cd apps/frontend && npm run typecheck`; `cd apps/frontend && npm run build`; `make vibe-verify`.
- Browser E2E outcome: skipped/not proven in this container because no browser binary, no Playwright/Cypress E2E dependency, and no Docker/Postgres runtime are available for a full super-admin bypass flow; use `superadmin@taskmind.local` / password `1` / OTP `1` once local services are running.
- Applicable skills: none. Delegated agents: none.

## 2026-06-06 (M04 Scheduler UI Slice)

### Added

- Added the scheduler frontend API/composable layer for Core-only scheduling preferences, generation, block list/update, and completion calls.
- Replaced the calendar placeholder with live scheduler states for loading, API errors, empty schedules, preference editing, generated block results, missed-block warnings, and reschedule proposal review.
- Added focused scheduler components for the calendar toolbar, preference form, scheduled block list, and reschedule proposal list.

### Frontend-visible change recorded

- `/calendar` now surfaces persisted scheduler-owned blocks from Core, lets users save optimistic-lock protected scheduling preferences, marks missed blocks prominently, supports block completion and time edits, and displays proposals returned by schedule generation.
- The scheduler feature continues to call only Core `/v1/scheduler/**` endpoints; no Relay or Nova direct frontend calls were introduced.

### Verification notes

- Exact verification commands recorded for the M04 UI slice: `cd apps/frontend && npm run typecheck`; `cd apps/frontend && npm run build`; `make vibe-verify`. The docs-only traceability update re-ran `make vibe-verify` successfully on 2026-06-06.
- Browser E2E outcome: skipped/not proven in this container because no browser binary, no Playwright/Cypress E2E dependency, and no Docker/Postgres runtime are available for a full `/calendar` super-admin bypass flow; use `superadmin@taskmind.local` / password `1` / OTP `1` once local services are running.
- Status: scheduler UI is implemented as a slice, but M04 must remain open until `make vibe-verify` and the `/calendar` browser flow both pass in milestone closeout.
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
- Added M08 AI feature composables and starter panels for capture, goal breakdown, weekly review, describe/autocomplete, translate, and Nova chat through Core facade endpoints.
