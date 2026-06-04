# M02 — Tasks & Projects (reference DDD modules)

## Objective

Build the two reference feature modules — **task** and **project** — fully through the DDD
four layers, with hierarchy, links, releases, assignees, soft-delete, optimistic locking,
and a synced OpenAPI contract. The `task` module is the canonical pattern every later module
mirrors.

## Depends on

[M01](M01-core-foundations.md).

## Scope

**In:** task CRUD + status/completion/archive + hierarchy (parent/level) + cross-task links
+ releases + assignee; project CRUD + membership + accent color + archive; the public
endpoints in [`../reference/api-contract.md`](../reference/api-contract.md) for tasks/projects;
`apps/backend/openapi.yaml`.

**Out:** scheduler, AI planning endpoints (those arrive in M04/M07), comments/attachments
(M09/M11-adjacent — comments/attachments are their own modules; build when their milestone
or a dependent milestone needs them).

## Files to create

```text
# Migrations
V11__add_task_key_column.sql
V15__add_task_assignee_id.sql
V25__add_task_hierarchy.sql                 # parent_task_id, task_level
V32__task_type_links_and_release_index.sql  # task_type, task_links table, release index
# (V9 story-points + V10 soft-delete may be folded in here if not done in M01)

# task module (reference DDD)
task/interfaces/rest/{TaskController,TaskLinkController,TaskLinkByIdController,
                      TaskReleaseController}.java
task/interfaces/rest/dto/{CreateTaskRequest,UpdateTaskRequest,TaskResponse,
                          UpdateTaskStatusRequest,TaskCompletionResponse,
                          CreateTaskLinkRequest,TaskLinkResponse,
                          ProjectReleaseResponse,ReleaseSummaryResponse}.java
task/application/{TaskApplicationService,TaskLinkApplicationService,
                  TaskReleaseApplicationService,TaskKeyAssigner,
                  CreateTaskCommand,UpdateTaskCommand}.java
task/domain/model/{Task,TaskLink,TaskStatus,TaskType,TaskLevel,TaskEffort,
                   TaskSource,TaskLinkType}.java
task/domain/{TaskHierarchyRules,TaskTypeRules,TaskLeafSqlFilters}.java
task/domain/repository/{TaskRepository,TaskLinkRepository}.java
task/infrastructure/persistence/jpa/{TaskJpaEntity,TaskLinkJpaEntity,
                                      JpaTaskRepository,JpaTaskLinkRepository,
                                      SpringDataTaskJpaRepository,
                                      SpringDataTaskLinkJpaRepository,
                                      TaskReleaseStatsProjection}.java

# project module
project/interfaces/rest/{ProjectController,ProjectMembershipController}.java
project/application/{ProjectApplicationService,ProjectMembershipApplicationService}.java
project/domain/model/{Project,ProjectMembership,ProjectMembershipRole,ProjectAccentColor}.java
project/domain/repository/{ProjectRepository,ProjectMembershipRepository}.java
project/infrastructure/persistence/jpa/{ProjectJpaEntity,ProjectMembershipJpaEntity,...}.java

# contract
apps/backend/openapi.yaml                  # tasks + projects groups
```

> Flyway note: migration names above preserve the reference intent. In the rebuild, follow
> the repository's current migration sequence and AGENTS.md's rule to append the next integer
> and never edit an applied migration.

## Key design notes

- Follow the layering and naming in [`../conventions.md`](../conventions.md) exactly — this
  module is the template.
- `task_key` is a human-readable key (`PROJ-123`) assigned by `TaskKeyAssigner`; enforce
  uniqueness via migration indexes.
- Hierarchy rules (`TaskHierarchyRules`) and type rules (`TaskTypeRules`) are pure domain
  logic with unit tests.
- Update endpoints use optimistic locking — stale writes return `409 Conflict`.
- Soft-delete via `deleted_at`; queries filter out deleted rows (`TaskLeafSqlFilters`).

## Acceptance criteria

- [ ] Create/read/update/delete task; set status; complete; archive; list with filters.
- [ ] Parent/child hierarchy + ancestors/children endpoints.
- [ ] Create/delete cross-task links; releases summary per project.
- [ ] Project CRUD + membership management + archive.
- [ ] Stale update returns 409 (covered by a test like `rejectsStaleTaskUpdate`).
- [ ] `openapi.yaml` matches the implemented task/project DTOs.

## Verification

```bash
mvn -q -Dtest='*Task*' test
mvn -q -Dtest='*Project*' test
make vibe-verify
```

## Definition of Done

Task + project modules complete through all four layers, contract synced, all tests green.
