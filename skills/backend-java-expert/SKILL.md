---
name: backend-java-expert
description: Expert backend Java software engineering for designing, implementing, testing, debugging, and reviewing Java services. Use when requests involve Java backend architecture, Spring Boot APIs, persistence, transactions, security, observability, performance, concurrency, refactoring, production readiness, or backend code review.
---

# Backend Java Expert

Use this skill to deliver production-grade Java backend work with clear boundaries, tests, and operational safeguards.

## Core workflow

1. **Clarify scope from artifacts**
   - Inspect the build files, framework versions, service boundaries, and local agent/repo instructions before editing.
   - Identify the owning module/service, public contracts, persistence layer, and tests affected by the request.
   - Prefer targeted changes in the smallest responsible layer; avoid cross-service or cross-module coupling unless explicitly required.

2. **Design before coding**
   - State the API, domain, persistence, and operational implications of the change.
   - Preserve backward compatibility unless the user explicitly asks for a breaking change.
   - Choose simple, idiomatic Java over clever abstractions; introduce patterns only when they reduce repeated complexity.

3. **Implement with backend discipline**
   - Keep controllers/adapters thin and move business rules into application/domain services.
   - Keep transaction boundaries explicit at the application service layer.
   - Validate inputs at boundaries and enforce invariants in domain code.
   - Use constructor injection, immutable DTOs/records where appropriate, and narrow interfaces.
   - Never hide import failures behind try/catch blocks.

4. **Protect data and contracts**
   - Treat schema migrations as append-only once applied.
   - Keep API specifications, DTOs, validation annotations, and tests synchronized.
   - Use optimistic locking or idempotency where concurrent writes can conflict.
   - Avoid secrets in code, logs, migrations, fixtures, or test output.

5. **Format after implementation**
   - Run the configured backend formatter from the owning service directory before final tests (for TaskMind backend: `mvn -q spotless:apply -DspotlessFiles=<changed-java-files>`).
   - Keep generated formatting-only churn separate from logic in your review notes so real behavior changes stay easy to inspect.

6. **Review the diff to find issues quickly**
   - Run `git diff --check` and inspect `git diff --stat` before tests to catch whitespace, unexpectedly broad changes, and generated noise.
   - Review changed files by owning layer: contract, auth, validation, transaction boundary, persistence mapping, migration, and test coverage.
   - Prefer fixing root causes in the responsible layer instead of patching symptoms across callers.

7. **Test like a backend owner**
   - Start with the smallest failing test that captures the behavior or bug.
   - Cover service/domain logic with unit tests and HTTP/persistence behavior with slice or integration tests as appropriate.
   - Add regression tests for bugs, boundary cases, authorization failures, transaction rollbacks, and migration-sensitive behavior.
   - Run targeted tests first, then the repository-required verification gate before finalizing.

## Task routing

- **API work**: Review the route, DTOs, validation, auth requirements, status codes, error shape, generated/manual OpenAPI docs, and controller tests.
- **Persistence work**: Review entity mappings, indexes, constraints, migration order, repository queries, transaction isolation, pagination, and N+1 risks.
- **Business logic**: Review invariants, state transitions, idempotency, race conditions, domain events, and test fixtures.
- **Security work**: Review authentication, authorization, tenant/user scoping, input validation, rate limits, sensitive logging, and dependency exposure.
- **Performance work**: Review query plans, batching, connection pools, cache invalidation, payload size, async boundaries, and measurable acceptance criteria.
- **Code review/debugging**: Reproduce or reason from tests/logs, locate the owning layer, identify root cause, fix minimally, and add a regression test.

## Reference loading

Read `references/java-backend-checklist.md` when the task is complex, high-risk, or needs a structured review checklist for API, persistence, transactions, security, performance, and testing.
