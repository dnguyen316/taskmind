# Vue Production Patterns

## Component patterns

- Keep templates declarative: move non-trivial branching, filtering, and formatting into `computed` values or small helper functions.
- Name events by user intent (`save`, `cancel`, `select-item`) rather than implementation (`click-button`).
- Prefer explicit `defineProps<T>()` and `defineEmits<T>()` signatures for reusable components.
- Use `withDefaults` for optional props that need runtime defaults.
- Avoid deep prop drilling by introducing a feature-level provider/composable or store only when intermediate components do not own the data.
- Keep two-way binding explicit: use `defineModel` where the project supports it, otherwise implement `modelValue`/`update:modelValue` consistently.

## Composables

- Name composables with `use*` and return stable, minimal APIs.
- Accept refs/getters when callers need reactivity; accept plain values for one-time configuration.
- Register cleanup with `onScopeDispose` for listeners, timers, observers, sockets, and subscriptions.
- Avoid making every composable global. Prefer local state unless cross-component reuse is required.
- Keep composables UI-agnostic unless their purpose is explicitly UI behavior such as focus traps, breakpoints, or keyboard shortcuts.

## Pinia stores

- Use stores for shared domain state, authenticated user/session state, cached server entities, or workflow state reused across routes.
- Keep transient local form state in components unless it must survive navigation.
- Expose actions for mutations that include business meaning; avoid having components mutate unrelated store internals.
- Reset store state on logout, tenant/workspace switch, or other security boundaries.

## Router and navigation

- Parse route params and query strings at page boundaries; convert them to typed internal values immediately.
- Keep guards small and deterministic. Delegate auth/session checks to stores or services.
- Preserve deep-linkability for filters, selected tabs, and search terms when users expect shareable URLs.
- Handle not found, unauthorized, and malformed-route states explicitly.

## Async data and API integration

- Centralize base URLs, auth headers, retries, and response/error normalization in an API client.
- Type API DTOs separately from view models when backend shapes do not match UI needs.
- Track request identity with an incrementing token, `AbortController`, or a query library when rapid changes can return out of order.
- Prefer explicit retry affordances over silent repeated failures.
- Make optimistic updates transactional: snapshot previous state, apply the UI change, then rollback or reconcile on failure.

## Forms

- Keep validation rules close to the form or in a feature-local module.
- Validate on submit at minimum; add blur/change validation when it improves correction speed without becoming noisy.
- Disable duplicate submission while a request is pending.
- Preserve user-entered values on validation or server errors.
- Focus the first invalid field or error summary for long forms.

## Performance

- Use stable `:key` values and avoid indexes for mutable lists.
- Virtualize long lists when rendering cost is user-visible.
- Debounce expensive search/filter requests and cancel stale requests.
- Use `shallowRef`/`markRaw` only for large objects or third-party instances where deep reactivity is wasteful.
- Lazy-load route-level chunks and heavy components when the project already supports code splitting.

## Testing and validation

- Prefer Vue Test Utils or the project's existing test stack for component behavior.
- Test user-visible behavior and emitted events, not implementation details.
- Include tests for loading, empty, error, disabled, and permission-sensitive states when applicable.
- Always run typecheck after changing props, emits, stores, routes, API types, or composables.
