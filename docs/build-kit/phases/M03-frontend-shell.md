# M03 — Frontend Shell

## Objective

Build the Vue SPA foundation and the first usable vertical slice: the axios `apiClient` with
token injection and transparent refresh, the Pinia auth store plus router guards, auth pages
(login/signup/forgot/profile), and the tasks plus projects pages wired to Core. After M03, a
user logs in through the browser and manages tasks/projects end to end.

## Depends on

[M02](M02-tasks-projects.md) (Core tasks/projects + auth APIs).

## Scope

**In:** `src/lib/*` HTTP layer, router + guards, auth store + pages, tasks feature
(list/board/detail), projects feature (list/detail/members), app shell (header/sidebar,
theme).

**Out:** scheduler/reports/AI/spec UIs (later milestones).

## Files to create

All paths in this section are relative to `apps/frontend`.

```text
src/main.ts                                      # createApp -> Pinia -> Antd -> router -> mount
src/App.vue                                      # a-config-provider (light/dark) + router-view
src/lib/apiClient.ts                             # axios bearer; intercept 401; single-flight refresh; ApiError
src/lib/authToken.ts                             # token storage
src/lib/authSession.ts                           # session-expired -> redirect
src/lib/apiError.ts                              # normalized ApiError
src/router/index.ts                              # routes + meta.requiresAuth/public; beforeEach guard
src/stores/auth.ts                               # session, login/signup/verify/oauth/logout/refresh/profile, ensureInitialized
src/stores/{pinia,projects,assignees}.ts
src/composables/{useTheme,useCurrentUserId}.ts

# auth feature
src/features/auth/pages/{AuthPage,ForgotPasswordPage,ProfileSettingsPage}.vue
src/features/auth/composables/useAuth.ts
src/features/auth/api/authApi.ts

# tasks feature (shell + core pages)
src/features/tasks/pages/{DashboardPage,TasksPage,TaskDetailPage}.vue
src/features/tasks/components/{AppHeader,AppSidebar,...}.vue
src/features/tasks/api/tasksApi.ts
src/features/tasks/constants/taskConstants.ts

# projects feature
src/features/projects/pages/{ProjectsDashboardPage,ProjectDetailPage}.vue
src/features/projects/components/{ProjectForm,ProjectMembers,...}.vue
src/features/projects/api/projectsApi.ts

# landing
src/features/landing/pages/LandingPage.vue
```

## Key design notes

- **All HTTP goes through `apiClient.ts`.** Use `VITE_API_BASE_URL`, defaulting to
  `http://localhost:8080`. Attach bearer tokens to protected requests. On `401` for a
  protected route, perform a single-flight refresh via `/v1/auth/token/refresh`, retry the
  original request once, and mark the session expired if refresh fails. Public auth
  endpoints are exempt from refresh retry behavior.
- **Router guards:** define `meta.requiresAuth` and `meta.public`. Run
  `authStore.ensureInitialized()` before each navigation. Redirect unauthenticated users
  to `/login?redirect=...`.
- **Task/project URLs:** use routes that preserve project context, including task detail
  under a project (for example `/projects/:projectId/tasks/:taskId`) and project detail
  (for example `/projects/:projectId`).
- **UI:** use Ant Design Vue 4. Keep pages thin; place reusable logic in composables and
  server calls in `api/` modules.
- **TypeScript:** keep API responses typed. `npm run typecheck` and `npm run build` must
  pass.

## Acceptance criteria

- [ ] Login/signup/logout work against Core in the browser.
- [ ] Protected routes redirect when unauthenticated; refresh keeps the session alive.
- [ ] Tasks: list, create, edit, status change, detail view.
- [ ] Projects: list, create, detail, members.
- [ ] `npm run typecheck` and `npm run build` pass.

## Verification

```bash
cd apps/frontend && npm run typecheck && npm run build
make vibe-verify
# Browser E2E on :5173 with superadmin@taskmind.local / password 1 / OTP 1:
# login -> create project -> create task -> change status -> logout
```

## Definition of Done

A user completes the auth + tasks + projects flow in the browser; typecheck/build green;
`make vibe-verify` passes.
