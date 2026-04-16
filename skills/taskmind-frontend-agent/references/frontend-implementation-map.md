# Frontend Implementation Map

Use this map to route frontend work quickly.

## App shell and routing

- `apps/frontend/src/main.ts`
- `apps/frontend/src/App.vue`
- `apps/frontend/src/router/index.ts`
- `apps/frontend/src/components/AppErrorBoundary.vue`

## Task feature structure

- Workspace entry:
  - `apps/frontend/src/features/tasks/TaskWorkspace.vue`
- Pages:
  - `apps/frontend/src/features/tasks/pages/TasksDashboardPage.vue`
  - `apps/frontend/src/features/tasks/pages/TaskDetailPage.vue`
- Components:
  - `apps/frontend/src/features/tasks/components/TaskList.vue`
  - `apps/frontend/src/features/tasks/components/TaskCreateForm.vue`
  - `apps/frontend/src/features/tasks/components/TaskFilters.vue`
- Composable:
  - `apps/frontend/src/features/tasks/composables/useTasks.ts`
- API module:
  - `apps/frontend/src/features/tasks/api/tasksApi.ts`
- Feature types/constants:
  - `apps/frontend/src/features/tasks/types.ts`
  - `apps/frontend/src/features/tasks/constants/taskConstants.ts`

## Shared client and styling

- HTTP client:
  - `apps/frontend/src/lib/apiClient.ts`
- Global styles:
  - `apps/frontend/src/style.css`

## Build/tooling files

- `apps/frontend/vite.config.ts`
- `apps/frontend/tsconfig.json`
- `apps/frontend/package.json`

When adding a new feature, mirror the same `pages/components/composables/api/types` structure used by the `tasks` feature.
