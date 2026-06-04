# M12 - Analytics, Dashboard & Team

## Objective

Surface the read models: reports/throughput KPIs from the Relay analytics rollups, an
aggregated home dashboard (with Redis cache), and a team directory. This closes the
analytics loop opened in M05/M06.

## Depends on

[M05](M05-eventing-relay.md) (analytics projections),
[M06](M06-search-storage-aws.md) (activity), and
[M03](M03-frontend-shell.md).

## Scope

**In:** Core `analytics` module (reports from rollups), `dashboard` module (aggregation +
cache), `team` module (directory), FE `reports` feature + dashboard widgets.

**Out:** hardening/deploy (M13).

## Files to create

```text
# Core analytics module
analytics/interfaces/rest/AnalyticsController.java              # /v1/reports
analytics/application/{AnalyticsApplicationService,ReportsResponse,ReportsKpis,
  ReportsRange,ReportsTrend,ReportsDeltas,ReportsSparklines,
  ReportsStatusSegment,ReportsPrioritySegment,
  ReportsProjectThroughput,ReportsAssigneeThroughput,
  ReportsAssigneeWorkload,ReportsTeamWorkload}.java
analytics/infrastructure/AnalyticsRollupRepository.java

# Core dashboard module
dashboard/interfaces/rest/DashboardController.java              # /v1/dashboard
dashboard/application/{DashboardApplicationService,DashboardResponse,KpiMetrics,
  MyTaskItem,...}.java
dashboard/infrastructure/DashboardCacheService.java             # Redis cache (flag-gated)

# Core team module
team/interfaces/rest/TeamController.java                        # /v1/team/directory
team/application/TeamApplicationService.java

# Frontend
src/features/reports/pages/ReportsPage.vue
src/features/reports/components/{WorkloadChart,ThroughputChart,...}.vue
src/features/reports/api/reportsApi.ts                          # incl. jsPDF PDF export
src/features/team/pages/TeamPage.vue
src/features/tasks/pages/DashboardPage.vue                      # wire real KPIs/widgets
```

Create these files under the owning app/module paths established by earlier milestones
(for example, Core modules live under `apps/backend`, and frontend features live under
`apps/frontend`). Keep `apps/backend/openapi.yaml` synchronized with any Core request or
response contract introduced here.

## Key design notes

- Reports read from the **Relay-populated** `analytics.*` rollups (daily user/project
  metrics). Core queries them through `AnalyticsRollupRepository`. Relay owns writes; Core
  owns the read API.
- Dashboard aggregation is cached in Redis (`taskmind.dashboard.cache.enabled`); invalidate
  on relevant events or TTL.
- Reports support week/month/quarter ranges, KPIs, trends, deltas, sparklines,
  status/priority segments, throughput, and workload by project/assignee/team.
- PDF export of reports is implemented in the frontend via jsPDF + jspdf-autotable.

## Acceptance criteria

- [ ] `GET /v1/reports?range=week|month|quarter` returns KPIs/trends/segments from
      rollups.
- [ ] `GET /v1/dashboard` returns aggregated KPIs, my-tasks, and an activity snippet
      (cached).
- [ ] `GET /v1/team/directory` returns the team aggregation.
- [ ] Reports UI renders charts and supports PDF export; dashboard shows real data; team
      page lists members.

## Verification

```bash
cd apps/backend && mvn -q -Dtest='*Analytics*,*Dashboard*,*Team*' test
make vibe-verify
# Browser E2E: open dashboard + reports (generate activity first), export PDF
```

## Definition of Done

Reports, Dashboard (cached), and team directory work off the analytics read models; UI
renders + exports; `make vibe-verify` green.
