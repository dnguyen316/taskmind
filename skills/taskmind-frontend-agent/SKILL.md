---
name: taskmind-frontend-agent
description: Implement and refine TaskMind frontend features in apps/frontend using Vue 3, TypeScript, vue-router, composables, feature-scoped APIs, and Ant Design Vue UI patterns. Use when requests involve page/component updates, form UX, client-side state handling, API integration, route changes, or frontend type-check/build validation.
---

# TaskMind Frontend Agent

Follow this workflow to deliver frontend changes consistently.

## 1) Load project context

- Read `apps/frontend/package.json` to confirm scripts and dependency versions.
- Read `apps/frontend/src/router/index.ts` before creating or modifying routes.
- Read `apps/frontend/src/lib/apiClient.ts` and `src/features/**/api/*.ts` before touching API integration.
- Read `references/frontend-implementation-map.md` in this skill for quick file targeting.

## 2) Pick the right feature location

- Put page-level UI in `src/features/<feature>/pages`.
- Put reusable feature components in `src/features/<feature>/components`.
- Put server calls in `src/features/<feature>/api`.
- Put reactive business logic in `src/features/<feature>/composables`.
- Put shared app shell or global-level wiring under `src/App.vue`, `src/main.ts`, and `src/router/index.ts`.

## 3) Implement with typed and testable boundaries

- Keep page components focused on composition/orchestration.
- Keep API modules thin and typed; centralize HTTP concerns through `lib/apiClient.ts`.
- Keep form constants and enums centralized (for example `constants/taskConstants.ts`).
- Keep route navigation and data loading predictable; prefer explicit loading/error states.
- Keep visual consistency with existing Ant Design Vue patterns.

## 4) Validate locally

From `apps/frontend` run:

1. `npm run typecheck`
2. `npm run build`

Use `npm run dev` for manual verification of route/page behavior when UI logic changed.

## 5) Delivery checklist

Before finalizing:

- Confirm component props/events remain type-safe.
- Confirm API payload/response shapes align with backend expectations.
- Confirm routes still resolve and navigation flows remain intact.
- Confirm build and typecheck pass for touched areas.
- If UI is visibly changed, capture a screenshot in environments where browser tooling is available.
