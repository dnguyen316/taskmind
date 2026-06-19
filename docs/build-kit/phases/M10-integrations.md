# M10 — Integrations (Jira / GitHub / Wiki)

## Objective

Connect external systems: Jira Cloud + GitHub (Enterprise) OAuth connections, project
links, import of external issues, publish of TaskMind tasks (Epic→Story→Subtask) to Jira,
external link tracking, and wiki connect. Tokens are encrypted at rest.

## Depends on

[M02](M02-tasks-projects.md), [M09](M09-spec-breakdown.md) (Jira publish mapping).

## Scope

**In:** Core `integration` module (connections, OAuth states, project links, import runs,
external links), migrations V20/V29, Jira/GitHub clients, token cipher, publish + import
application services, FE `integrations` feature.

**Out:** notifications (M11).

## Files to create

```text
# Migrations
V20__create_integrations_tables.sql                 # connections, oauth_states, project_links,
                                                    # import_runs, external_links
V29__integration_project_link_metadata.sql          # metadata_json on project links

# Core integration module
integration/interfaces/rest/{IntegrationController,TaskJiraPublishController}.java
integration/application/{IntegrationConnectionApplicationService,
  IntegrationProjectLinkApplicationService,IntegrationImportApplicationService,
  IntegrationPublishApplicationService,IntegrationExternalLinkApplicationService}.java
integration/domain/model/{IntegrationConnection,IntegrationProjectLink,
  IntegrationImportRun,IntegrationExternalLink,...}.java
integration/domain/repository/{...}.java
integration/infrastructure/jira/JiraCloudClient.java
integration/infrastructure/github/GitHubClient.java
integration/infrastructure/security/TokenCipher.java
integration/infrastructure/persistence/jpa/{...}.java

# Frontend
src/features/integrations/pages/SettingsIntegrationsPage.vue
src/features/integrations/components/{ConnectDialog,ProjectLinkForm,ImportDialog,...}.vue
src/features/integrations/api/integrationsApi.ts
```

## Key design notes

- OAuth: store transient `integration_oauth_states`; callbacks are **public** routes
  (`/v1/integrations/{jira,github}/oauth/callback`) — add them to the security allow-list.
- Encrypt access/refresh tokens at rest with `TokenCipher`
  (`TASKMIND_INTEGRATIONS_TOKEN_KEY`).
- Import is bounded (`TASKMIND_INTEGRATIONS_IMPORT_LIMIT`, default 500). Jira, GitHub, and wiki transports use the linked connection base URL plus the decrypted bearer access token; provider failures are surfaced as stable problem details with `code`, `providerStatus`, and `retrySafe`. Repeated Jira/GitHub imports are idempotent: existing `external_links` for the same provider, `ISSUE` resource type, and external id or key are skipped and reflected in `skippedCount` instead of creating duplicate tasks.
- Publish maps TaskMind hierarchy to Jira Epic→Story→Subtask, idempotent on external key
  (reuse the mapping defined in M09 / `docs/architecture/jira-scrum-publish.md`).
- `external_links` track the TaskMind↔external relationship for re-sync.

## Acceptance criteria

- [ ] Connect Jira/GitHub via OAuth; tokens stored encrypted.
- [ ] Link a TaskMind project to an external project with metadata.
- [ ] Import external issues (bounded) into tasks.
- [ ] Publish a task hierarchy to Jira idempotently; external links recorded.
- [ ] Integrations settings UI manages connections/links/import/publish.

## Verification

```bash
cd apps/backend && mvn -q -Dtest='*Integration*,*JiraPublish*' test
make vibe-verify
# Browser E2E (mock/stub external): connect, link project, import, publish
```

## Definition of Done

Integrations module complete with encrypted tokens, OAuth, import, idempotent publish, and
UI; `make vibe-verify` green.
