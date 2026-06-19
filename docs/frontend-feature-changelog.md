# Frontend Feature Changelog

## 2026-06-19 (Dedicated Password Recovery UI)

### Changed

- Added a dedicated `/forgot-password` recovery page with Ant Design Vue request, accepted, and error states instead of routing the path through the shared login/signup form.
- Added typed frontend helpers for Core's documented public `/v1/auth/password/{flow}` password recovery endpoint and wired the forgot-password route to the new page.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run format -- src/features/auth/pages/ForgotPasswordPage.vue src/features/auth/pages/AuthPage.vue src/features/auth/api/authApi.ts src/router/index.ts ../../docs/frontend-feature-changelog.md`.
- Targeted frontend verification: `cd apps/frontend && npm run typecheck && npm run build`.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused auth recovery UI update.
- Applicable skills: none. Delegated agents: none.

## 2026-06-19 (Task Detail Client Validation)

### Changed

- Added client-side task detail validation before save so blank titles, too-long titles, out-of-range priorities, invalid durations, and invalid due dates surface through the existing task detail error alert without calling Core.
- Centralized task form validation limits for create and detail task forms under `apps/frontend/src/features/tasks/validation/`.

### Verification notes

- Frontend coverage for this slice is through formatting and `vue-tsc --noEmit`; there is no frontend test runner configured yet.
- Browser E2E outcome: not run in this container; no browser automation was requested for this form-validation slice.
- Applicable skills: none. Delegated agents: none.

## 2026-06-19 (M12 Dashboard and Reports Completion)

### Changed

- Wired the Dashboard page to Core `GET /v1/dashboard` with typed response adaptation, loading/error states, live KPI cards, my-task rows, and activity snippets from the cached dashboard aggregation.
- Added Reports throughput/workload chart components and an Export PDF action backed by jsPDF + jspdf-autotable so M12 reports can be rendered and exported from live Core report data.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run format -- src/features/reports/components/ThroughputChart.vue src/features/reports/components/WorkloadChart.vue src/features/reports/api/reportsApi.ts src/features/reports/pages/ReportsPage.vue src/features/dashboard/types.ts src/features/dashboard/api/dashboardApi.ts src/features/dashboard/composables/useDashboard.ts src/features/tasks/pages/DashboardPage.vue ../../docs/frontend-feature-changelog.md`.
- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused M12 frontend completion update.
- Applicable skills: none. Delegated agents: none.

## 2026-06-18 (Team and Reports Core Integration)

### Changed

- Wired the Team page to Core `GET /v1/team/directory` with typed response adaptation, a directory composable, loading/error/empty states, and live member/workload rendering.
- Wired the Reports page to Core `GET /v1/reports?range=week|month|quarter` with typed response adaptation, a reports composable, range switching, loading/error/empty states, KPI cards, and workload/throughput tables.
- Updated the sidebar Team and Reports entries to remove coming-soon badges now that they navigate to live Core-backed pages.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run format -- src/features/team/pages/TeamPage.vue src/features/team/types.ts src/features/team/api/teamApi.ts src/features/team/composables/useTeamDirectory.ts src/features/reports/pages/ReportsPage.vue src/features/reports/types.ts src/features/reports/api/reportsApi.ts src/features/reports/composables/useReports.ts src/features/tasks/components/AppSidebar.vue ../../docs/frontend-feature-changelog.md`.
- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Full gate verification: `make vibe-verify`.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused Core integration update.
- Applicable skills: none. Delegated agents: none.

## 2026-06-18 (Task Attachment Multi-Upload)

### Changed

- Updated the task attachment panel to allow selecting multiple files at once, show the first selected filename with an additional-file count, and upload the selected files through the existing Core attachment API.
- Attachment uploads now append all successful metadata responses to the panel and report partial failures when only some selected files upload successfully.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run format -- src/features/tasks/components/TaskAttachmentsPanel.vue ../../docs/frontend-feature-changelog.md`.
- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Full gate verification: `make vibe-verify`.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused attachment-panel update.
- Applicable skills: none. Delegated agents: none.

## 2026-06-16 (Dashboard Shell Visual Polish)

### Changed

- Removed the dashboard search helper text so the desktop topbar stays compact.
- Updated sidebar navigation styling to remove link underlines, use distinct Dashboard and Tasks icons, strengthen active colors, and make typography feel more modern.
- Rounded the signed-in user avatar into a fixed circle and refreshed the floating AI assistant with a compact purple launcher and modern chat panel styling.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run format -- src/features/tasks/components/AppSidebar.vue src/features/tasks/pages/DashboardPage.vue src/components/AiAssistantWidget.vue src/style.css ../../docs/frontend-feature-changelog.md`.
- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Targeted frontend verification: `cd apps/frontend && npm run build`.
- Full gate verification: `make vibe-verify` rerun and failed in existing backend `SchedulerControllerTest.schedulerGenerateOwnsPlanningBehaviorAndPersistsWithoutDuplicates` while frontend checks passed.
- Browser E2E outcome: not run in this container; manual verification should compare Dashboard with the provided screenshot at desktop width, open/close the AI widget, and confirm the mobile drawer still works.
- Applicable skills: none. Delegated agents: none.

## 2026-06-16 (Dashboard SaaS Shell Slots)

### Changed

- Added named authenticated-shell slots for page title, subtitle, desktop header actions, and default content.
- Reworked the desktop authenticated shell into a compact SaaS grid with a fixed sidebar column, sticky desktop page topbar, and scrollable main content while keeping the existing mobile topbar and drawer behavior.
- Moved the dashboard title, subtitle, search affordance, disabled notification shortcut, and new-task action into the new shell header slots.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run format -- src/features/tasks/components/AppLayout.vue src/features/tasks/pages/DashboardPage.vue ../../docs/frontend-feature-changelog.md`.
- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Targeted frontend verification: `cd apps/frontend && npm run build`.
- Full gate verification: `make vibe-verify` failed in existing backend `SchedulerControllerTest.schedulerGenerateOwnsPlanningBehaviorAndPersistsWithoutDuplicates`; frontend typecheck and build passed.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused shell layout update. Manual verification should open Dashboard on desktop and mobile widths, confirm header slot content appears in the desktop topbar, and confirm the mobile drawer still opens and closes.
- Applicable skills: none. Delegated agents: none.

## 2026-06-16 (AI Workspace Hero Copy)

### Changed

- Replaced the internal milestone tag on the AI capture page with a user-facing workspace availability label.
- Rewrote the page hero to describe the currently rendered AI capture, goal breakdown, and weekly review capabilities.
- Moved forward-looking AI workspace copy into a collapsed secondary upcoming-features area.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run format -- src/features/ai/pages/InboxCapturePage.vue ../../docs/frontend-feature-changelog.md`.
- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused copy update. Manual verification should open the AI workspace and confirm the hero, tag, and collapsed upcoming section read without milestone-coded primary copy.
- Applicable skills: none. Delegated agents: none.

## 2026-06-16 (Project Archive Safety)

### Changed

- Added a confirmation step before archiving projects from the project dashboard list.
- Tracked per-project archive pending state so active archive buttons disable consistently while a create/save/archive operation is running.
- Moved project feedback into the active project list card so archive success and failure messages appear next to the affected list.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Full gate verification: `make vibe-verify` failed in existing backend `SchedulerControllerTest.schedulerGenerateOwnsPlanningBehaviorAndPersistsWithoutDuplicates`; frontend typecheck and build passed.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused project-list safety change. Manual verification should open Projects, click Archive project, cancel once, then confirm and verify inline success/error feedback.
- Applicable skills: none. Delegated agents: none.

## 2026-06-16 (M08 Nova Chat Streaming UI)

### Added

- Updated the Nova chat composable to call Core-only `POST /v1/nova/chat/stream` with `Accept: text/event-stream`.
- Assistant responses now append incoming SSE chunks incrementally while preserving the returned Nova `sessionId` across subsequent chat turns.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Browser E2E outcome: skipped/not proven in this container because no browser automation was requested for this slice; use the super-admin bypass once local services are running.
- Applicable skills: none. Delegated agents: none.

# Frontend Feature Changelog

## 2026-06-19 (Task Detail Client Validation)

### Changed

- Added client-side task detail validation before save so blank titles, too-long titles, out-of-range priorities, invalid durations, and invalid due dates surface through the existing task detail error alert without calling Core.
- Centralized task form validation limits for create and detail task forms under `apps/frontend/src/features/tasks/validation/`.

### Verification notes

- Frontend coverage for this slice is through formatting and `vue-tsc --noEmit`; there is no frontend test runner configured yet.
- Browser E2E outcome: not run in this container; no browser automation was requested for this form-validation slice.
- Applicable skills: none. Delegated agents: none.

## 2026-06-16 (Sidebar Navigation Availability Labels)

### Changed

- Replaced milestone-coded sidebar badges with short user-facing availability labels for workspace navigation.
- Added native tooltip copy for planned or preview workspace routes so users see availability context without developer milestone IDs in primary navigation.

### Frontend-visible change recorded

- Workspace sidebar entries now show concise labels such as Soon, Preview, and Beta instead of internal milestone references.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run format -- src/features/tasks/components/AppSidebar.vue ../../docs/frontend-feature-changelog.md`.
- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Targeted frontend verification: `cd apps/frontend && npm run build`.
- Full gate verification: `make vibe-verify` failed in existing backend `SchedulerControllerTest.schedulerGenerateOwnsPlanningBehaviorAndPersistsWithoutDuplicates`; frontend typecheck passed.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused navigation copy update. Manual verification should hover or focus workspace navigation items and confirm concise availability labels fit in desktop and mobile sidebar layouts.
- Applicable skills: none. Delegated agents: none.

## 2026-06-16 (Nova Assistant Widget Polish)

### Changed

- Removed duplicate route visibility checks from the Nova assistant widget so app-level routing owns where it appears.
- Added an accessible floating button label, visible panel close action, empty-state copy, surfaced chat errors, and separate user/Nova message bubbles.

### Frontend-visible change recorded

- Authenticated routes show a more accessible Nova assistant widget with clearer message layout and inline failure feedback, while unauthenticated routes continue to hide it from App-level routing.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Full gate verification: `make vibe-verify` failed in existing backend `SchedulerControllerTest.schedulerGenerateOwnsPlanningBehaviorAndPersistsWithoutDuplicates`; frontend typecheck and build passed.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused widget polish. Manual verification should open an authenticated route, open Nova, confirm the empty state and close control, send a message, and confirm user/Nova bubbles render.
- Applicable skills: none. Delegated agents: none.

## 2026-06-15 (M12 Dashboard Trust Polish)

### Changed

- Reworked the dashboard so primary cards show only live task metrics, live task rows, and working Core/SPA links.
- Moved unavailable analytics, AI insight, notification, workload, and project health areas into a compact collapsed “Upcoming features” roadmap with disabled/coming-soon copy.
- Removed realistic fake people, task activity, and project names from primary dashboard UI areas to avoid mock data being mistaken for live data.

### Verification notes

- Target checks for this UI polish: `cd apps/frontend && npm run format -- src/features/tasks/pages/DashboardPage.vue`; `cd apps/frontend && npm run typecheck`; `cd apps/frontend && npm run build`; `make vibe-verify`.
- Browser E2E proof remains skipped in this container unless a browser-capable local service stack is available.
- Applicable skills: none. Delegated agents: none.

## 2026-06-15 (Dashboard Search Affordance)

### Changed

- Wired the dashboard search field to submit non-empty queries to the authenticated Activity search route with a `q` query parameter, and taught Activity search to hydrate from that URL query.
- Added helper copy explaining that dashboard search uses Relay activity search, and disabled the placeholder notification button so it is no longer an active control without behavior.

### Frontend-visible change recorded

- Authenticated users can type a dashboard search term, press Enter or the search button, and navigate to Activity search with that query in the URL.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Full gate verification: `make vibe-verify` failed in existing backend `SchedulerControllerTest.schedulerGenerateOwnsPlanningBehaviorAndPersistsWithoutDuplicates`; frontend typecheck and build passed.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused dashboard affordance fix. Manual verification should submit a non-empty dashboard search and confirm navigation to `/activity?q=<term>`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-15 (Authenticated Shell Mobile Navigation)

### Changed

- Added a responsive authenticated-shell top bar below 1200px with the TaskMind brand, current workspace page title, and an accessible menu button.
- Rendered the existing app sidebar inside an Ant Design Vue drawer for mobile navigation while preserving the desktop sidebar.
- Added sidebar navigation events so the mobile drawer closes after route navigation and logout remains available from the drawer.

### Frontend-visible change recorded

- Authenticated users on tablet and mobile widths can open the workspace navigation from the top bar, navigate to any shell route, and log out without relying on the hidden desktop sidebar.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Full gate verification: `make vibe-verify` failed in existing backend `SchedulerControllerTest.schedulerGenerateOwnsPlanningBehaviorAndPersistsWithoutDuplicates`; frontend typecheck and build passed.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused shell responsiveness change. Manual verification should resize below 1200px, open the navigation drawer, tab to the menu and logout controls, and confirm route navigation closes the drawer.
- Applicable skills: none. Delegated agents: none.

## 2026-06-15 (M03 Active Project Task Creation)

### Changed

- Wired the tasks page active project into the task creation form as the default project selection.
- Updated the task creation form to apply a newly loaded default project after projects hydrate and to block submission until at least one project exists.

### Frontend-visible change recorded

- Creating a task from the Tasks page now preselects the active project once projects finish loading, and users without projects see helper copy with a link to create their first project.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Full gate verification: `make vibe-verify` failed in existing backend `SchedulerControllerTest.schedulerGenerateOwnsPlanningBehaviorAndPersistsWithoutDuplicates`; frontend typecheck and build passed.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused task creation UX change.
- Applicable skills: none. Delegated agents: none.

## 2026-06-15 (M03 Task Detail Direct Lookup)

### Changed

- Replaced task detail loading via broad list search with a direct Core `GET /v1/tasks/{id}` request adapted through the existing task response mapper.
- Simplified the task detail composable lookup call so it no longer passes list pagination or user scoping parameters for detail retrieval.

### Frontend-visible change recorded

- Task detail pages now load the requested task directly from Core instead of fetching a larger task list and searching client-side.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Full gate verification: `make vibe-verify` failed in existing backend `SchedulerControllerTest.schedulerGenerateOwnsPlanningBehaviorAndPersistsWithoutDuplicates`; frontend typecheck and build passed.
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
- Full gate verification: `make vibe-verify` failed in existing backend `SchedulerControllerTest.schedulerGenerateOwnsPlanningBehaviorAndPersistsWithoutDuplicates`; frontend typecheck and build passed.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused auth-session state change. Manual verification should seed an expired access token and valid refresh token, confirm the protected request retries successfully, and confirm `authStore.session.expiresAt` updates without clearing `currentUser`.
- Applicable skills: none. Delegated agents: none.

## 2026-06-14

- Refactored the Projects dashboard into the shared app shell, removed the user-facing owner-id field from project creation, and aligned project creation/list styling with TaskMind surface-card layout patterns.

# Frontend Feature Changelog

## 2026-06-19 (Task Detail Client Validation)

### Changed

- Added client-side task detail validation before save so blank titles, too-long titles, out-of-range priorities, invalid durations, and invalid due dates surface through the existing task detail error alert without calling Core.
- Centralized task form validation limits for create and detail task forms under `apps/frontend/src/features/tasks/validation/`.

### Verification notes

- Frontend coverage for this slice is through formatting and `vue-tsc --noEmit`; there is no frontend test runner configured yet.
- Browser E2E outcome: not run in this container; no browser automation was requested for this form-validation slice.
- Applicable skills: none. Delegated agents: none.

## 2026-06-15 (Authenticated Shell Mobile Navigation)

### Changed

- Added a responsive authenticated-shell top bar below 1200px with the TaskMind brand, current workspace page title, and an accessible menu button.
- Rendered the existing app sidebar inside an Ant Design Vue drawer for mobile navigation while preserving the desktop sidebar.
- Added sidebar navigation events so the mobile drawer closes after route navigation and logout remains available from the drawer.

### Frontend-visible change recorded

- Authenticated users on tablet and mobile widths can open the workspace navigation from the top bar, navigate to any shell route, and log out without relying on the hidden desktop sidebar.

### Verification notes

- Targeted frontend verification: `cd apps/frontend && npm run typecheck`.
- Full gate verification: `make vibe-verify` failed in existing backend `SchedulerControllerTest.schedulerGenerateOwnsPlanningBehaviorAndPersistsWithoutDuplicates`; frontend typecheck and build passed.
- Browser E2E outcome: not run in this container; no browser automation was requested for this focused shell responsiveness change. Manual verification should resize below 1200px, open the navigation drawer, tab to the menu and logout controls, and confirm route navigation closes the drawer.
- Applicable skills: none. Delegated agents: none.

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
- Full gate verification: `make vibe-verify` failed in existing backend `SchedulerControllerTest.schedulerGenerateOwnsPlanningBehaviorAndPersistsWithoutDuplicates`; frontend typecheck and build passed.
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

## 2026-06-19 (Task Detail Optimistic Lock Version)

### Changed

- The task API adapter now parses Core task response `version` values into the frontend `Task` model.
- Task detail form state preserves the loaded task version and sends it with detail updates, enabling Core stale-update conflict detection from the SPA.

### Verification notes

- Frontend coverage for this slice is currently through `vue-tsc --noEmit`; there is no frontend test runner configured yet.
- Applicable skills: none. Delegated agents: none.
