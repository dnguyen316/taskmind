# M05 - Eventing & Relay

## Objective

Wire the event-driven backbone for TaskMind: the shared `libs/events` envelope, Core's
transactional outbox, a Redis Streams publisher, and the Relay service consumer that
projects events into the Postgres `analytics` schema. This milestone unlocks analytics,
activity feeds, and read-side context for later AI features.

## Depends on

- [M02 - Tasks + Projects](M02-tasks-projects.md).
- Reference: [`../reference/domain-events.md`](../reference/domain-events.md).
- AGENTS guardrails: keep service boundaries intact; Core owns writes and facade APIs,
  Relay owns analytics projections and read-context, and frontend traffic still goes only
  through Core.

## Scope

**In:**

- `libs/events`: domain event envelope, event type constants/registry, JSON schema,
  validator, mapper, and transport port.
- Core outbox + events modules: write domain events in the same transaction as task and
  project state changes, persist activity events, publish unprocessed outbox rows to Redis
  Stream `taskmind.events`, and expose operational outbox/DLQ/export behavior where the
  milestone requires it.
- Core Flyway migrations for the eventing data plane: activity events, outbox events, and
  the `analytics` schema/tables that Relay writes to. Core Flyway owns these migrations;
  Relay must not auto-create schema objects.
- Relay ingest, sink, projection, DLQ, export, and observability packages.
- Relay analytics projections, including `analytics.event_store`, daily metrics tables,
  and user/project context read models.
- Relay internal read APIs under `/internal/context/**` for Nova's future context reads.
- Relay internal route security under `/internal/**` using service-token authentication.
- End-to-end integration coverage proving Core outbox publish -> Redis stream -> Relay
  ingest -> analytics projection.

**Out:**

- OpenSearch indexing and activity search APIs; these are part of M06.
- S3 object storage and attachment flows; these are part of M06.
- Nova AI consumption of Relay context; build the Relay context endpoints here, but wire
  Nova usage in M08.
- Frontend UI changes; this milestone is backend/library infrastructure only.

## Files to create or extend

Use these as the expected implementation areas. Keep package names consistent with the
existing M00-M02 module layout and the DDD layering conventions in the build kit.

```text
# libs/events
libs/events/src/main/java/.../DomainEvent.java
libs/events/src/main/java/.../EventTypes.java
libs/events/src/main/java/.../EventTypeRegistry.java
libs/events/src/main/java/.../DomainEventMapper.java
libs/events/src/main/java/.../DomainEventValidator.java
libs/events/src/main/java/.../transport/EventTransport.java
libs/events/src/main/resources/schema/domain-event-v1.json

# Core migrations
apps/backend/src/main/resources/db/migration/V12__create_activity_events_table.sql
apps/backend/src/main/resources/db/migration/V13__create_outbox_events_table.sql
apps/backend/src/main/resources/db/migration/V14__create_analytics_schema.sql

# Core outbox + events
apps/backend/src/main/java/.../outbox/application/OutboxEventWriter.java
apps/backend/src/main/java/.../outbox/application/OutboxPollerJob.java
apps/backend/src/main/java/.../outbox/application/OutboxPipelineMetrics.java
apps/backend/src/main/java/.../outbox/infrastructure/OutboxEventJpaEntity.java
apps/backend/src/main/java/.../outbox/infrastructure/OutboxEventJpaRepository.java
apps/backend/src/main/java/.../outbox/infrastructure/RedisStreamEventTransport.java
apps/backend/src/main/java/.../outbox/infrastructure/SpringDataOutboxEventJpaRepository.java
apps/backend/src/main/java/.../events/TaskDomainEventPublisher.java
apps/backend/src/main/java/.../events/ProjectDomainEventPublisher.java
apps/backend/src/main/java/.../activity/application/ActivityEventRecorder.java
apps/backend/src/main/java/.../activity/domain/model/ActivityEvent.java
apps/backend/src/main/java/.../activity/domain/model/ActivityEventType.java
apps/backend/src/main/java/.../activity/domain/repository/ActivityEventRepository.java
apps/backend/src/main/java/.../activity/infrastructure/persistence/jpa/ActivityEventJpaEntity.java
apps/backend/src/main/java/.../activity/infrastructure/persistence/jpa/JpaActivityEventRepository.java
apps/backend/src/main/java/.../activity/infrastructure/persistence/jpa/SpringDataActivityEventJpaRepository.java
apps/backend/src/main/java/.../activity/infrastructure/persistence/jpa/ActivityJsonConverters.java

# Relay service
apps/relay/src/main/java/.../relay/ingest/StreamEventConsumerJob.java
apps/relay/src/main/java/.../relay/ingest/IngestApplicationService.java
apps/relay/src/main/java/.../relay/ingest/IngestController.java
apps/relay/src/main/java/.../relay/sink/EventStoreWriter.java
apps/relay/src/main/java/.../relay/projection/TaskProjectionHandler.java
apps/relay/src/main/java/.../relay/projection/ProjectProjectionHandler.java
apps/relay/src/main/java/.../relay/projection/DailyMetricsProjector.java
apps/relay/src/main/java/.../relay/dlq/RelayDeadLetterWriter.java
apps/relay/src/main/java/.../relay/export/RelayExportService.java
apps/relay/src/main/java/.../relay/observability/RelayPipelineMetrics.java
apps/relay/src/main/java/.../relay/context/ContextQueryService.java
apps/relay/src/main/java/.../relay/context/ContextController.java
apps/relay/src/main/java/.../relay/security/RelaySecurityConfig.java
```

## Key design notes

- Build `libs/events` first, then Core outbox, then the Relay consumer.
- Core must write the outbox row **in the same transaction** as the corresponding task or
  project state change from M02.
- Treat Relay processing as **idempotent**. Deduplicate on `eventId`; projection retries
  must be safe.
- Persist every successfully consumed event to `analytics.event_store` before or while
  updating derived analytics projections.
- Failed Relay projection attempts must land in `analytics.relay_dlq` with enough payload
  and error context to replay or diagnose the failure.
- `/internal/**` on Relay requires service-token authentication via `RelaySecurityConfig`.
- Context read APIs under `/internal/context/**` are built here because M08 Nova features
  need them later; do not add Nova consumers in this milestone.
- Honor Redis Streams backpressure. Polling/consumer configuration should avoid unbounded
  in-memory buffering and should leave unprocessed events retryable.
- Keep a `RelayStreamPipelineIntegrationTest`-style integration test with Testcontainers
  Redis to prove end-to-end ingest -> projection behavior.
- Keep Core `apps/backend/openapi.yaml` in sync only if this milestone changes Core's
  public request/response contracts. Internal Relay endpoints do not make the frontend call
  Relay directly.

## Acceptance criteria

- [ ] Creating a task emits a Core outbox event in the same database transaction as the
      task write.
- [ ] Updating a task emits a Core outbox event in the same database transaction as the
      task write.
- [ ] Creating or updating a project emits a Core outbox event in the same database
      transaction as the project write.
- [ ] The outbox poller publishes unpublished events to Redis Stream `taskmind.events` and
      marks successfully published rows without losing retryability for failed publishes.
- [ ] Relay consumes from `taskmind.events`, honors backpressure, validates/mapping-checks
      the `libs/events` envelope, and deduplicates on `eventId`.
- [ ] Relay writes consumed events to `analytics.event_store` and updates user/project
      daily metrics projections.
- [ ] Failed Relay projections are written to `analytics.relay_dlq`.
- [ ] Relay context endpoints under `/internal/context/**` return scoped data for a user
      and/or project and require service-token authentication.
- [ ] Integration tests prove Core outbox publish -> Redis stream -> Relay ingest ->
      analytics projection.

## Verification

Run targeted checks while building, then run the full milestone gate before marking M05
complete.

```bash
# Core outbox/event tests
cd apps/backend && mvn -q -Dtest='*Outbox*,*Event*,*Activity*' test

# Relay stream/projection/ingest tests
cd apps/relay && mvn -q -Dtest='*Stream*,*Projection*,*Ingest*,*Context*' test

# Repository quality gate
make vibe-verify
```

## Definition of Done

Outbox -> Redis -> Relay -> analytics projections is proven by tests; Relay context
endpoints are live and protected; DLQ and idempotency behavior are in place; and
`make vibe-verify` is green.
