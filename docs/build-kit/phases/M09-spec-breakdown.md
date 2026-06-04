# M09 — Spec Breakdown Pipeline

## Objective

Deliver the **spec-to-backlog pipeline**: a product-manager workspace where a product spec
is turned into an **Epic → Story → Subtask** hierarchy through asynchronous AI jobs
(`outline → enrich → breakdown → merge`), with reusable templates, rich content,
attachments, pause/cancel/checkpoint controls, and materialization into real Core tasks.

This milestone includes Nova spec capabilities and the Core-side Jira/Scrum publish field
mapping. The actual external Jira/GitHub OAuth, import, and transport integrations are
implemented in M10.

## Depends on

- **M08 — AI features**: Nova capability pattern, Core AI facades, deterministic mock AI
  behavior, and AI event flow.
- **M06 — Search + Storage on AWS**: attachment storage through the Core object-storage
  abstraction backed by Amazon S3 in production.
- **M02 — Tasks + Projects**: task hierarchy support and project-owned task state.
- **Reference contracts**: the AI capability catalog and API/event/data-model references
  under `docs/build-kit/reference/` once those reference files exist in this rebuild kit.

## Scope

**In:**

- Core `specbreakdown` module with asynchronous processing jobs, a worker, templates,
  draft attachments, and task materialization.
- Flyway-owned Core migrations for spec breakdown drafts, job state, workspace rich
  content, Scrum/Jira fields, pause/cancel controls, and AI job type tracking.
- Nova spec capabilities:
  - `spec_breakdown`
  - `spec_breakdown_section`
  - `spec_outline`
  - `spec_enrich`
  - `spec_suggest_links`
  - `spec_merge`
- Frontend `specbreakdown` feature workspace with a tree editor, AI panels, attachment
  handling, and a publish dialog.
- Jira/Scrum field mapping for publish payloads and task materialization metadata.

**Out:**

- Generic Jira/GitHub OAuth connection flows, import flows, and external publish transport
  implementation. Those belong to **M10 — Integrations**.

## Files to create

> Migration numbers below describe the intended sequence after the previous milestone. If
> the implementation already has later migrations, keep the same logical migrations but use
> the next available `V<n>__description.sql` numbers. Never edit an applied migration.

```text
# Core migrations (apps/backend)
V24__create_spec_breakdown_drafts_table.sql
V26__spec_breakdown_async_jobs.sql
V27__spec_breakdown_workspace.sql                 # rich content, templates, attachments tables
V28__task_scrum_jira_fields.sql
V30__spec_breakdown_pause_cancel.sql
V31__spec_breakdown_ai_job_types.sql

# Core specbreakdown module (apps/backend)
specbreakdown/interfaces/rest/SpecBreakdownController.java        # /v1/spec-breakdown/**
specbreakdown/application/SpecBreakdownApplicationService.java
specbreakdown/application/SpecBreakdownWorker.java
specbreakdown/application/SpecBreakdownProcessingJob.java
specbreakdown/domain/model/SpecBreakdownDraft.java
specbreakdown/domain/model/SpecBreakdownStatus.java
specbreakdown/domain/model/SpecBreakdownJobStatus.java
specbreakdown/domain/repository/SpecBreakdownDraftRepository.java
specbreakdown/domain/repository/SpecBreakdownJobRepository.java
specbreakdown/config/SpecBreakdownConfig.java
specbreakdown/config/SpecBreakdownProperties.java
specbreakdown/template/SpecBreakdownTemplateController.java       # /v1/projects/{id}/spec-templates
specbreakdown/attachment/SpecBreakdownAttachmentController.java
specbreakdown/infrastructure/persistence/jpa/*.java

# Nova spec capabilities (apps/ai)
capability/SpecBreakdownCapability.java
capability/SpecBreakdownSectionCapability.java
capability/SpecBreakdownMergeCapability.java
capability/SpecOutlineCapability.java
capability/SpecEnrichCapability.java
capability/SpecSuggestLinksCapability.java
capability/SpecBreakdownPromptSupport.java

# Frontend (apps/frontend)
src/features/specbreakdown/pages/SpecWorkspacePage.vue
src/features/specbreakdown/components/SpecTreeEditor.vue
src/features/specbreakdown/components/AiPanels.vue
src/features/specbreakdown/components/JiraPublishDialog.vue
src/features/specbreakdown/api/specBreakdownApi.ts
```

## Core behavior

### Draft workspace

A spec breakdown draft belongs to a project and stores the current working hierarchy before
it becomes real tasks. The draft must support:

- Raw spec text and structured rich content.
- Attachments using the existing attachment/object-storage abstraction.
- A versioned working tree of candidate Epics, Stories, and Subtasks.
- Template selection and template-specific fields.
- Scrum/Jira metadata such as fix version, affected version, sprint, issue type, and
  publish keys where applicable.
- Optimistic locking on mutable entities.

### Async job pipeline

Spec processing is asynchronous. Core owns job records and exposes facade endpoints; Nova
owns prompts, provider calls, and deterministic mock responses.

The processing flow is:

1. `outline` — produce a high-level Epic/Story outline from the spec.
2. `enrich` — add details, acceptance notes, risks, estimates, labels, and metadata.
3. `breakdown` — decompose Stories into Subtasks and normalize the hierarchy.
4. `merge` — merge AI output into the draft without losing user edits.

Jobs must support:

- Queue limits, concurrency limits, timeouts, and retry behavior configured through
  `SpecBreakdownProperties`.
- `pause`, `resume`, and `cancel` commands.
- Durable checkpoints so a paused or restarted worker can continue without losing the
  last safe state.
- An `ai_job_type` discriminator covering `outline`, `enrich`, `breakdown`, `section`,
  and `merge` jobs.
- Clear terminal statuses for success and failure.

### Materialization

Materializing a draft creates real Core tasks in the existing task hierarchy:

- The output hierarchy is **Epic → Story → Subtask**.
- Created tasks link back to the originating `spec_breakdown_draft_id`.
- Scrum/Jira fields are copied into task metadata when available.
- Materialization must be transactional enough that partial writes do not leave an
  inconsistent hierarchy.
- The operation emits domain events so Relay and downstream consumers can project the new
  activity.

### Events

Emit events that are compatible with the existing Core outbox and Relay pipeline:

- `ai.spec_breakdown_completed`
- `ai.spec_breakdown_failed`

When materialization creates or updates tasks, emit the same task/project domain events
used by the M02/M05 task lifecycle instead of introducing duplicate event semantics.

## Nova behavior

Nova implements the spec-related capabilities through the existing M07/M08 capability
pattern. All prompts and provider calls remain inside `apps/ai`; Core only calls Nova
through service-token-protected facades.

Required capabilities:

| Capability | Purpose |
|------------|---------|
| `spec_outline` | Convert a product spec into a draft Epic/Story outline. |
| `spec_enrich` | Add descriptions, acceptance criteria, estimates, labels, risks, and metadata. |
| `spec_breakdown` | Produce a complete Epic → Story → Subtask hierarchy. |
| `spec_breakdown_section` | Process one selected section of a larger spec. |
| `spec_merge` | Merge AI output into the existing user-edited workspace tree. |
| `spec_suggest_links` | Suggest likely related tasks, projects, docs, or dependencies. |

Mock-provider behavior must remain deterministic so targeted tests can validate the full
pipeline without real LLM calls.

## Frontend behavior

The frontend adds a `specbreakdown` feature. It still talks **only** to Core.

The workspace should let a user:

- Create and edit a spec draft.
- Attach files to the draft.
- Pick, create, update, and delete project-scoped templates.
- Run outline, enrich, section breakdown, full breakdown, and merge actions.
- Pause, resume, or cancel a running job.
- See job progress, terminal status, and error details.
- Edit the generated Epic/Story/Subtask tree before materialization.
- Materialize the hierarchy into real tasks.
- Open a Jira publish dialog that uses the M09 field mapping; external Jira transport is
  completed in M10.

## API and contract notes

- Core endpoints live under `/v1/spec-breakdown/**` for the spec workspace.
- Project template endpoints live under `/v1/projects/{id}/spec-templates/**`.
- Frontend calls Core only; Core delegates AI work to Nova.
- Keep `apps/backend/openapi.yaml` synchronized with every Core request or response shape
  introduced in this milestone.
- Internal service calls must use the existing service-token model for `/internal/**`
  routes.

## Acceptance criteria

- [ ] A user can create a spec draft and run `outline → enrich → breakdown` jobs
  asynchronously.
- [ ] A user can pause, cancel, and resume a job, and checkpoints are persisted.
- [ ] Project-scoped template CRUD works.
- [ ] Draft attachments work through the existing storage abstraction.
- [ ] A user can materialize the generated Epic → Story → Subtask hierarchy into real
  tasks with Scrum/Jira fields.
- [ ] `ai.spec_breakdown_completed` and `ai.spec_breakdown_failed` events are emitted.
- [ ] The spec workspace UI renders the tree, runs AI panels, and opens the publish flow.
- [ ] Core OpenAPI stays in sync with the new request/response DTOs.

## Verification

```bash
cd apps/backend && mvn -q -Dtest='*SpecBreakdown*' test
cd apps/ai && mvn -q -Dtest='*Spec*' test
make vibe-verify
# Browser E2E: paste a spec -> outline -> breakdown -> materialize tasks
```

## Definition of Done

Async spec pipeline, templates, attachments, and materialization work end to end; Nova spec
capabilities are deterministic under the mock provider; Core events flow through the
existing event pipeline; `make vibe-verify` is green; and browser E2E confirms a full
`spec → tasks` run.
