# Java Backend Checklist

Use this checklist selectively; do not turn every small change into a large refactor.

## Architecture and layering

- Confirm the change belongs in the selected service/module.
- Keep inbound adapters responsible for transport concerns only.
- Keep application services responsible for orchestration, authorization checks, transactions, and calls to ports.
- Keep domain objects responsible for business invariants and state transitions.
- Keep infrastructure adapters responsible for persistence, messaging, clients, serialization, and framework integration.

## API and validation

- Define request/response DTOs explicitly; do not expose persistence entities directly.
- Validate required fields, size limits, enum values, pagination limits, and cross-field rules.
- Return consistent status codes and error payloads.
- Preserve compatibility for existing clients unless a breaking change is intentional.
- Update OpenAPI or equivalent API documentation when contracts change.

## Persistence and migrations

- Add a new migration for schema changes; do not edit applied migrations.
- Prefer database constraints for durable invariants and application checks for clear errors.
- Add indexes for new lookup paths and foreign keys where query patterns require them.
- Review cascade rules and orphan removal carefully.
- Verify queries for pagination stability, N+1 problems, and tenant/user scoping.

## Transactions and consistency

- Put write transaction boundaries around complete use cases, not individual repository calls.
- Avoid external network calls inside database transactions unless unavoidable.
- Use optimistic locking, idempotency keys, unique constraints, or explicit locks for concurrent commands.
- Publish events after durable state changes using the project’s established outbox/event pattern when available.

## Security and operations

- Enforce authentication and authorization at the boundary or application service before data access.
- Scope reads/writes by tenant, organization, owner, or principal where applicable.
- Avoid logging secrets, tokens, passwords, personal data, or full request bodies with sensitive fields.
- Ensure configuration comes from environment/profile-managed properties, not hard-coded secrets.
- Add metrics/logs/tracing at meaningful boundaries when diagnosing production behavior.

## Testing strategy

- Unit-test pure domain rules and application branching.
- Use controller tests for HTTP validation, auth, status codes, and serialization.
- Use persistence tests for mappings, custom queries, migrations, and constraints.
- Add regression tests for bugs before the fix when possible.
- Run targeted tests first, then the repository’s full quality gate.
