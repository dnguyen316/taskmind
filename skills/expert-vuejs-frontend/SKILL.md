---
name: expert-vuejs-frontend
description: Expert Vue.js frontend engineering workflow for Vue 3, Composition API, TypeScript, Vite, Pinia, vue-router, component architecture, state management, forms, accessibility, performance, testing, debugging, code reviews, and production-quality UI implementation. Use when Codex needs to build, refactor, review, or diagnose Vue frontend features, single-file components, composables, stores, routes, API integration, or frontend build/typecheck failures.
---

# Expert Vue.js Frontend

Use this skill to deliver production-grade Vue frontend work with strong type safety, maintainable architecture, accessible UI, and reliable validation.

## Workflow

1. **Map the app first**
   - Read `package.json` to identify Vue, Vite, TypeScript, UI library, state, router, and test scripts.
   - Read the relevant route, page, component, composable, store, API client, and type files before editing.
   - Preserve the project's existing conventions unless they conflict with correctness or explicit user requirements.

2. **Choose the right boundary**
   - Put presentational UI in components with typed props and emits.
   - Put page orchestration in route/page views.
   - Put reusable reactive logic in composables.
   - Put shared cross-page state in Pinia stores only when state must outlive a component tree or be reused across routes.
   - Put server calls in API/client modules; avoid raw `fetch`/Axios calls scattered through components.

3. **Implement with Composition API and TypeScript discipline**
   - Prefer `<script setup lang="ts">` for single-file components.
   - Type props, emits, exposed methods, API payloads, route params, and store state explicitly.
   - Use `computed` for derived state; use `watch` only for side effects or bridging external systems.
   - Keep mutations easy to trace; avoid mutating props and avoid hidden module-level mutable state.

4. **Design robust async UX**
   - Model loading, empty, success, and error states deliberately.
   - Cancel or ignore stale async results when inputs change quickly.
   - Surface actionable validation and API errors near the relevant UI.
   - Keep optimistic updates reversible when server persistence can fail.

5. **Protect accessibility and usability**
   - Use semantic elements before custom widgets.
   - Ensure keyboard access, focus management, visible focus states, labels, alt text, and ARIA only when semantic HTML is insufficient.
   - Verify forms expose clear required, invalid, disabled, and submitting states.

6. **Validate before finishing**
   - Run the smallest relevant checks first, then the project quality gate when practical.
   - Prefer `npm run typecheck`, `npm run test`, `npm run build`, and any project-specific lint/E2E commands discovered in `package.json`.
   - For visible UI changes, inspect manually or capture a screenshot when browser tooling is available.

## Decision guide

- **Component getting large?** Extract child components for independent UI sections, or composables for reusable behavior. Do not extract only to hide complexity.
- **State duplicated across siblings?** Lift state to the nearest owner. Use Pinia when multiple routes/features need the same source of truth.
- **Computed or watch?** Use `computed` for values. Use `watch` for side effects, debounced remote calls, persistence, or imperative integrations.
- **Prop or slot?** Use props for data/configuration; use slots for caller-controlled layout or content.
- **API type mismatch?** Fix the typed API boundary first, then simplify downstream component assumptions.

## Code review checklist

- Confirm the component has one clear responsibility and no accidental business logic in templates.
- Confirm prop defaults, emits, and `v-model` contracts are typed and documented by names.
- Confirm route guards, query/param parsing, and redirects handle missing or malformed values.
- Confirm composables clean up intervals, event listeners, subscriptions, observers, and pending async work.
- Confirm list rendering uses stable keys and avoids unnecessary re-renders for large collections.
- Confirm user-facing text, dates, numbers, and errors follow existing i18n/formatting conventions if present.
- Confirm generated DOM remains accessible and responsive across likely viewport sizes.

## References

Read `references/vue-production-patterns.md` when implementing or reviewing complex Vue work that involves component design, composables, Pinia, router, async data, forms, performance, or tests.
