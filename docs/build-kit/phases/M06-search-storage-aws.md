# M06 — Search & Storage on AWS

## Objective

Add the two AWS-managed data-plane capabilities required by the TaskMind target
architecture:

- **Amazon OpenSearch Service** for activity search. Relay owns the activity indexer;
  Core exposes the user-facing read API.
- **Amazon S3** for task attachments. Core owns attachment metadata and object-storage
  access through a storage port.

Both capabilities must run locally without AWS code changes: OpenSearch/Elasticsearch for
search, and LocalStack S3 or the filesystem adapter for object storage. Production swaps
only configuration and credentials.

## Depends on

- [M05 — Eventing + Relay](M05-eventing-relay.md): domain events flow from Core to Relay.
- [M02 — Tasks + Projects](M02-tasks-projects.md): task/project ownership and task routes
  exist in Core.
- [00 — Product & Architecture Overview](../00-overview.md): service boundaries and the
  local-to-AWS data-plane mapping.

## Scope

**In:**

- Relay `search/` module for activity-event indexing and query support.
- Core `activity/search/` facade API that the frontend can call.
- Core `attachment/` module for task attachment metadata, upload, download, and delete.
- S3-backed `ObjectStoragePort` adapter plus local/test fallback.
- Flyway migration for task attachment metadata.
- Frontend hooks/components for task attachments and activity search.

**Out:**

- Notifications and integrations; those are implemented in later milestones.
- Nova AI context retrieval beyond the activity/search surfaces introduced here.
- Production infrastructure provisioning; this milestone makes the application AWS-ready,
  while M13 owns the full AWS deployment story.

## Files to create

Use the normal package root for each Java service. The paths below are module-relative
intent, not a license to cross service boundaries.

```text
# Relay search -> OpenSearch
apps/relay/src/main/java/.../search/ElasticsearchConfig.java
apps/relay/src/main/java/.../search/ActivityEventDocument.java
apps/relay/src/main/java/.../search/ActivityEventSearchRepository.java
apps/relay/src/main/java/.../sink/ElasticsearchIndexer.java

# Core activity search API
apps/backend/src/main/java/.../activity/search/ActivitySearchController.java
apps/backend/src/main/java/.../activity/search/ActivitySearchApplicationService.java
apps/backend/src/main/java/.../activity/search/ActivitySearchDocument.java
apps/backend/src/main/java/.../activity/search/ActivitySearchRepository.java
apps/backend/src/main/java/.../activity/search/ActivitySearchElasticsearchConfig.java

# Core attachments + S3
apps/backend/src/main/resources/db/migration/V21__create_task_attachments_table.sql
apps/backend/src/main/java/.../attachment/interfaces/rest/TaskAttachmentController.java
apps/backend/src/main/java/.../attachment/application/TaskAttachmentApplicationService.java
apps/backend/src/main/java/.../attachment/domain/model/TaskAttachment.java
apps/backend/src/main/java/.../attachment/domain/model/MediaKind.java
apps/backend/src/main/java/.../attachment/domain/repository/TaskAttachmentRepository.java
apps/backend/src/main/java/.../attachment/domain/repository/ObjectStoragePort.java
apps/backend/src/main/java/.../attachment/infrastructure/persistence/jpa/*.java
apps/backend/src/main/java/.../attachment/infrastructure/storage/S3ObjectStorageAdapter.java
apps/backend/src/main/java/.../attachment/infrastructure/storage/FilesystemObjectStorageAdapter.java
apps/backend/src/main/java/.../attachment/config/AttachmentStorageConfig.java
apps/backend/src/main/java/.../attachment/config/AttachmentStorageProperties.java

# Frontend
apps/frontend/src/features/tasks/components/TaskAttachments*.vue
apps/frontend/src/features/activity/*
```

## API surfaces

### Core task attachments

Expose task-scoped attachment endpoints from Core only:

```text
POST   /v1/tasks/{taskId}/attachments
GET    /v1/tasks/{taskId}/attachments
GET    /v1/tasks/{taskId}/attachments/{attachmentId}/download
DELETE /v1/tasks/{taskId}/attachments/{attachmentId}
```

Requirements:

- The metadata row belongs to the task and user/team authorization context already owned
  by Core.
- Upload validates configured size limits and `MediaKind` before storing the object.
- Delete removes or tombstones the metadata and deletes the backing object when possible.
- Download returns the attachment through Core using the configured storage adapter.
- Keep `apps/backend/openapi.yaml` in sync with request and response DTOs.

### Core activity search

Expose activity search through Core so the frontend never calls Relay directly:

```text
GET /v1/activity/search
```

Requirements:

- The endpoint requires user authentication like other `/v1/**` routes.
- The endpoint searches activity events when search is enabled.
- If search is disabled by configuration, the stack must still boot and the API must fail
  predictably rather than creating hard OpenSearch startup dependencies.

## Key design notes

- **OpenSearch:** configure Spring Data Elasticsearch with
  `spring.elasticsearch.uris` pointing to the OpenSearch endpoint. Production must be able
  to add SigV4 signing or fine-grained authentication without changing application code.
- **Index name:** Relay indexes activity documents into `activity-events`.
- **Conditional search:** OpenSearch/Elasticsearch beans must be conditional on a
  configured search repository. The application must run without search when disabled;
  the `test` profile should exclude OpenSearch/Elasticsearch autoconfiguration unless a
  test explicitly enables it.
- **Feature flag:** use `TASKMIND_ACTIVITY_SEARCH_ENABLED` or an equivalent typed
  configuration property to switch activity search on/off.
- **S3:** `S3ObjectStorageAdapter` is the default non-test storage adapter. In production
  it uses the IAM task role and the AWS default credential chain. Local development can
  point the same adapter at LocalStack with path-style access and an endpoint override.
- **Tests:** tests use the filesystem storage adapter unless they explicitly exercise S3
  behavior through LocalStack/Testcontainers.
- **Storage port:** Core business logic depends on `ObjectStoragePort`, not directly on
  AWS SDK classes.
- **Validation:** attachment upload validates media kind and configured size limits via
  `AttachmentStorageProperties`.
- **Service boundary:** Relay owns activity indexing. Core owns task attachments and the
  frontend-facing activity-search API; any Core OpenSearch access here is read-only and
  scoped to that facade. The frontend continues to call Core only.

## Acceptance criteria

- [ ] Relay indexes `task.*` domain events into the OpenSearch `activity-events` index.
- [ ] `GET /v1/activity/search` returns matching activity results when search is enabled.
- [ ] The stack boots when search is disabled.
- [ ] Task attachment upload, list, download, and delete work through Core.
- [ ] Attachment objects land in S3/LocalStack when the S3 adapter is configured.
- [ ] Tests run with the filesystem storage adapter and without requiring AWS credentials.
- [ ] Attachment metadata is created by a new Flyway migration and Hibernate validates the
      schema.
- [ ] Core request/response changes are reflected in `apps/backend/openapi.yaml`.
- [ ] Frontend task attachment and activity-search UI hooks typecheck.

## Verification

```bash
cd apps/relay && mvn -q -Dtest='*Search*,*Indexer*' test
cd apps/backend && mvn -q -Dtest='*Attachment*,*ActivitySearch*' test
make vibe-verify
# Browser E2E: attach a file to a task; search activity from the UI.
```

## Definition of Done

OpenSearch activity search and S3 task attachments work locally and are AWS-ready.
Conditional beans keep tests green and allow the stack to boot with search disabled.
`make vibe-verify` passes, and the browser E2E proves the attachment and activity-search
flows through the frontend.
