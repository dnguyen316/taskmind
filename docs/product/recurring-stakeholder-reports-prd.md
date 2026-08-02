# Recurring Stakeholder Reports Product Specification

## 1. Purpose and product fit

TaskMind must help managers prepare, approve, and distribute trustworthy recurring
stakeholder reports from the work already recorded in TaskMind. This capability extends
the existing weekly-review and project-brief AI flows, Relay-backed reports and dashboard,
Core notifications, and email and Slack delivery. It is not a separate reporting product,
parallel data store, or general-purpose document builder.

The reporting experience must:

- reuse weekly review as the personal and team reporting starting point;
- reuse project brief as the project and customer-delivery narrative starting point;
- embed existing dashboard KPIs, trends, project health, workload, and freshness states;
- use the notification center and preferences for update requests, approval reminders,
  delivery receipts, and escalations;
- use the existing email and Slack channel adapters alongside in-app delivery; and
- preserve the established boundary in which the frontend calls only Core, Relay owns
  analytics projections and read context, Nova owns prompts and generated interpretation,
  and Core owns workflow, authorization, approvals, snapshots, and delivery orchestration.

### Goals

1. Reduce recurring report preparation and follow-up effort without lowering factual
   reliability.
2. Give every material claim traceable evidence, freshness, and fact-versus-interpretation
   labeling.
3. Keep owners and approvers in control of incomplete inputs, edits, approval, and
   external distribution.
4. Serve operational, project, customer, and executive audiences through governed
   templates rather than one unconstrained prompt.

### Non-goals

- A standalone report database that duplicates Core entities or Relay rollups.
- A free-form custom report or prompt builder in the pilot.
- Autonomous changes to tasks, projects, milestones, ownership, risks, or decisions.
- Treating email or Slack message history as an authoritative work system.
- Financial forecasting, employee performance scoring, or surveillance.

## 2. Users and responsibilities

| Role                             | Responsibility                                                                                                               |
| -------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| Report owner                     | Configures scope, template, cadence, channels, approver, and policy; resolves workflow exceptions.                           |
| Contributor or responsible owner | Supplies or confirms missing updates for assigned work, milestones, KPIs, blockers, dependencies, decisions, and risks.      |
| Approver                         | Reviews material changes and unresolved gaps, edits or regenerates sections, and approves or rejects distribution.           |
| Recipient                        | Reads the distributed report within the permissions and confidentiality policy of the report.                                |
| Workspace administrator          | Configures allowed templates, channels, confidentiality mappings, retention, reminders, delegation, and escalation policies. |

One person may hold multiple roles, but approval rules must not be bypassed implicitly. If
policy requires separation of duties, the report owner cannot approve their own report.

## 3. Report scopes

A report configuration must select exactly one scope and resolve it at generation time.
Every included source must be visible to the requesting user, approver, and intended
recipient population; report generation must not broaden source permissions.

| Scope                          | Included context                                                                                                                                              | Default owner                                        | Guardrails                                                                                                                       |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| Individual workstream          | One person's work or one explicitly configured stream of tasks, activities, milestones, decisions, risks, and dependencies.                                   | Workstream owner or line manager                     | Must not become a performance score; private tasks and restricted projects remain excluded.                                      |
| Project                        | One Core project, its permitted child work, members, milestones, health, decisions, risks, dependencies, and Relay project rollups.                           | Project manager or project owner                     | Cross-project dependencies appear only when the viewer may access both sides; otherwise show a restricted dependency warning.    |
| Team                           | A configured Core team and its permitted active projects, workstreams, capacity/workload, milestones, dependencies, decisions, risks, and Relay team rollups. | Team manager                                         | Individual detail is minimized for broad audiences; aggregate where policy requires it.                                          |
| Portfolio or executive summary | An explicit set of projects or teams with aggregate health, KPI movements, strategic milestones, cross-scope dependencies, risks, and decisions.              | Portfolio lead, chief of staff, or executive sponsor | No inferred organization-wide access; each included scope and recipient group must be authorized and confidentiality-compatible. |

Scopes are saved references, not copied datasets. Membership and permissions are
re-evaluated when each draft is generated, refreshed, approved, and delivered. A scope
change after approval creates a new draft/version and invalidates the prior approval.

## 4. Governed report templates

The following templates are first-class configurations built from the shared section
model in section 5. Administrators may disable a template or narrow its channels, but
pilot users cannot remove required sections, weaken confidentiality, or bypass approval
requirements.

### 4.1 Weekly team update

| Attribute               | Requirement                                                                                                                                                                                                                                       |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Intended audience       | Team members, the team's manager, and invited internal partner leads.                                                                                                                                                                             |
| Reporting cadence       | Weekly; default Monday through Sunday in the workspace time zone, generated before the configured distribution time.                                                                                                                              |
| Required data sections  | Executive summary; progress against the prior week; completed and upcoming milestones; KPI changes; blockers and dependencies; scope/timeline/ownership changes; decisions required; risks and mitigations; freshness and missing-input warnings. |
| Tone and maximum length | Direct, collaborative, action-oriented, and non-evaluative; maximum 800 words excluding evidence labels, tables, and warnings.                                                                                                                    |
| Required approver       | Team manager or configured delegate.                                                                                                                                                                                                              |
| Distribution channels   | In-app required; email and Slack optional according to workspace and recipient preferences.                                                                                                                                                       |
| Confidentiality level   | Internal by default; may be raised to Restricted, never lowered below the highest included source classification.                                                                                                                                 |

### 4.2 Project steering update

| Attribute               | Requirement                                                                                                                                                                                                                                                                             |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Intended audience       | Project sponsor, steering group, project leadership, and accountable workstream owners.                                                                                                                                                                                                 |
| Reporting cadence       | Biweekly or monthly, configurable; reporting boundaries must remain stable between periods.                                                                                                                                                                                             |
| Required data sections  | Steering summary; prior-period progress; completed/upcoming milestones; delivery and health KPI changes; blockers and internal/external dependencies; scope/timeline/ownership changes; decisions required with owner and due date; risks and proposed mitigations; freshness and gaps. |
| Tone and maximum length | Concise, decision-led, neutral, and explicit about variance; maximum 1,200 words excluding evidence labels, tables, and warnings.                                                                                                                                                       |
| Required approver       | Project sponsor or designated steering chair.                                                                                                                                                                                                                                           |
| Distribution channels   | In-app required; email optional; Slack optional for internal steering groups.                                                                                                                                                                                                           |
| Confidentiality level   | Confidential by default; Restricted when any included source or workspace policy requires it.                                                                                                                                                                                           |

### 4.3 Executive portfolio summary

| Attribute               | Requirement                                                                                                                                                                                                                                                                                                                          |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Intended audience       | Executives, portfolio governance, chief of staff, and explicitly authorized senior leaders.                                                                                                                                                                                                                                          |
| Reporting cadence       | Monthly by default; quarterly is supported.                                                                                                                                                                                                                                                                                          |
| Required data sections  | Portfolio headline; prior-period outcomes; strategic completed/upcoming milestones; aggregate KPI changes with drill-down evidence; material blockers and cross-portfolio dependencies; scope/timeline/ownership changes; decisions/investments required; top risks and mitigations; freshness, coverage, and missing-data warnings. |
| Tone and maximum length | Strategic, succinct, evidence-led, and free of operational detail unless material; maximum 1,000 words excluding evidence labels, tables, and warnings.                                                                                                                                                                              |
| Required approver       | Portfolio owner, chief of staff, or executive sponsor named in configuration.                                                                                                                                                                                                                                                        |
| Distribution channels   | In-app required; email optional; Slack disabled by default and permitted only for an approved private channel.                                                                                                                                                                                                                       |
| Confidentiality level   | Restricted by default.                                                                                                                                                                                                                                                                                                               |

### 4.4 Customer delivery update

| Attribute               | Requirement                                                                                                                                                                                                                                                                                                                         |
| ----------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Intended audience       | Authorized customer stakeholders plus internal account and delivery leadership.                                                                                                                                                                                                                                                     |
| Reporting cadence       | Weekly or biweekly, aligned to the customer governance agreement.                                                                                                                                                                                                                                                                   |
| Required data sections  | Customer-safe summary; progress against the prior period; delivered and upcoming milestones; agreed customer-facing KPI changes; customer-visible blockers and dependencies; approved scope/timeline/ownership changes; customer decisions required; customer-relevant risks and mitigations; freshness and missing-input warnings. |
| Tone and maximum length | Clear, accountable, non-technical unless requested, and free of internal-only commentary; maximum 900 words excluding evidence labels, tables, and warnings.                                                                                                                                                                        |
| Required approver       | Accountable delivery lead; customer success or account owner is a required co-approver when workspace policy enables external distribution separation of duties.                                                                                                                                                                    |
| Distribution channels   | In-app for internal reviewers; email for authorized customer recipients; Slack only for an explicitly linked customer channel approved by policy.                                                                                                                                                                                   |
| Confidentiality level   | External Confidential; included evidence must be explicitly customer-shareable and must not expose internal Restricted content.                                                                                                                                                                                                     |

### 4.5 Incident or recovery update

| Attribute               | Requirement                                                                                                                                                                                                                                                                                     |
| ----------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Intended audience       | Incident leadership, affected internal stakeholders, and—only through an externally approved variant—authorized customers.                                                                                                                                                                      |
| Reporting cadence       | At the configured incident interval while active, then once at recovery/closure; cadence may be hourly or daily rather than calendar-recurring.                                                                                                                                                 |
| Required data sections  | Current state and impact; change since the previous update; completed and next recovery milestones; service/recovery KPI changes; blockers and dependencies; scope/timeline/incident-ownership changes; decisions required; active risks and mitigations; freshness and missing-input warnings. |
| Tone and maximum length | Factual, calm, time-stamped, unambiguous, and free of speculation presented as fact; maximum 600 words excluding evidence labels, tables, and warnings.                                                                                                                                         |
| Required approver       | Incident commander or configured recovery lead; external variants additionally require communications or account approval according to policy.                                                                                                                                                  |
| Distribution channels   | In-app required; email and Slack allowed by incident communication policy.                                                                                                                                                                                                                      |
| Confidentiality level   | Restricted by default; an External Confidential variant must be separately approved and contain only shareable evidence.                                                                                                                                                                        |

## 5. Shared content contract

Every template must render all required content below. A section can state that no
verified content is available, but it cannot silently disappear. Section ordering and
labels may vary to fit the audience.

| Required content                                | Minimum behavior                                                                                                                                                                                                                       |
| ----------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Progress against the previous period            | Compare the effective period to its immediately preceding equivalent period. Identify outcomes, meaningful status movement, and unchanged material commitments; label the comparison unavailable when a valid baseline does not exist. |
| Completed and upcoming milestones               | List milestones completed within the period and the next material milestones with owner, target date, status, and linked Core entity.                                                                                                  |
| KPI changes                                     | Show current value, prior comparable value, absolute or percentage change when mathematically valid, unit, Relay metric identity, aggregation/range, and availability/freshness state.                                                 |
| Blockers and dependencies                       | Show status, severity/materiality, responsible owner, affected scope or milestone, expected resolution, and supporting task/activity/risk.                                                                                             |
| Scope, timeline, or ownership changes           | Identify what changed, previous and current values, effective date, actor or recorded source, and impact. No change must be inferred solely from silence.                                                                              |
| Decisions required                              | State the decision, accountable decision-maker, options or recommendation when recorded, deadline, consequence of delay, and supporting decision/task/risk.                                                                            |
| Risks and proposed mitigations                  | Separate recorded risk facts from Nova-proposed interpretation; include probability/impact when recorded, owner, mitigation, due date, and evidence.                                                                                   |
| Data freshness and missing-information warnings | Show the report-wide source-data timestamp, per-source freshness/availability where material, stale threshold, missing owners or expected sources, and effect on confidence/completeness.                                              |

Every report also shows its title, template, scope, effective reporting period (inclusive
start and exclusive end, with time zone), draft/snapshot version, generated/refreshed
time, approver and approval time, confidentiality label, recipient/channel summary, and
source-data timestamp.

## 6. Evidence, provenance, and truthfulness rules

### 6.1 Claim evidence

1. **Quantitative statements:** Every number, percentage, trend, count, duration, date
   variance, or other quantitative claim must reference either a Relay metric or a Core
   entity field. Its evidence chip or footnote must include source type, stable source ID,
   field/metric name, aggregation and filters when applicable, and observed timestamp.
2. **Qualitative statements:** Every assertion about progress, health, cause, impact,
   confidence, intent, priority, completion, delay, or expected outcome must identify at
   least one supporting Core task, activity, decision, or risk. Multiple sources may be
   attached; unsupported claims are marked as unresolved and cannot be converted to fact
   by editing their prose alone.
3. **Generated interpretation:** Nova-written synthesis, recommendation, causal reading,
   risk proposal, or forecast must be visually distinct from recorded fact in edit,
   approval, in-app snapshot, email, Slack, and export views. Use an explicit
   **AI-generated interpretation** label and presentation treatment; never rely on color
   alone. Recorded facts use a **Recorded fact** label and evidence link.
4. **Time context:** Every draft and snapshot must show the effective reporting period and
   source-data timestamp. Each materially stale source gets its own warning even when the
   report-wide timestamp is recent.
5. **Absence is unknown, not proof:** Missing activity cannot be described as "no work,"
   "no progress," or equivalent. The system must say that no qualifying activity was
   found in the available sources for the period, identify missing/stale sources, and
   request confirmation when the distinction is material.

### 6.2 Evidence lifecycle

- Evidence references are captured when a draft is generated and recomputed on refresh.
- Inline edits retain their evidence links. Removing or materially changing a supported
  claim requires selecting replacement evidence, marking it as approver-authored
  interpretation, or leaving it as an unresolved unsupported claim that blocks delivery
  unless policy explicitly allows incomplete reports.
- Section regeneration may use only the current authorized scope, reporting period,
  confirmed owner responses, and selected evidence. It cannot regenerate untouched
  sections or silently discard approver edits.
- Approval binds the rendered content, evidence manifest, freshness state, warnings,
  approver identity, recipient/channel plan, and confidentiality label to one version.
- Distributed snapshots retain resolvable evidence identifiers and a display-safe copy of
  the cited fact sufficient for audit even if the live entity later changes. Access to the
  underlying live entity remains permission-checked.

## 7. Operating workflow and states

### 7.1 Required workflow

1. **Generate a scheduled draft.** Core triggers the saved schedule idempotently, resolves
   scope and permissions, obtains Relay metrics/read context, invokes the existing Nova
   weekly-review or project-brief capability pattern for governed sections, and saves a
   versioned draft. Scheduling never distributes directly from generation.
2. **Identify missing or stale inputs.** The draft validator evaluates required sections,
   source availability, source timestamps, owner coverage, evidence completeness, and
   configured freshness thresholds. It records gaps rather than substituting zeros or
   invented narrative.
3. **Request updates from responsible owners.** Core creates actionable notifications
   using existing notification preferences and in-app, email, or Slack adapters. Each
   request identifies the report, section, requested fields, deadline, and secure in-app
   response link.
4. **Refresh after responses arrive.** Accepted owner responses become attributed report
   inputs linked to the relevant Core evidence. Refresh recomputes affected metrics and
   regenerates only eligible sections while preserving manual edits elsewhere.
5. **Present changes and gaps.** Before approval, the review view highlights material
   differences from the previous draft and previous distributed snapshot, resolved and
   unresolved gaps, stale sources, degraded dependencies, and changed recipients or
   confidentiality.
6. **Allow editing and section regeneration.** Authorized reviewers can edit inline,
   attach/replace evidence, restore a prior draft section, or regenerate one section with
   an optional instruction. All actions are versioned and audited.
7. **Require configured approval.** Required approvers approve the exact version. Any
   material content, evidence, period, scope, confidentiality, or recipient change after
   approval returns the report to approval; spelling-only edits may follow an explicitly
   configured non-material-change policy.
8. **Deliver through configured channels.** Core renders a channel-appropriate form and
   delivers through in-app, email, or Slack using notification/channel preferences,
   authorization, confidentiality, retry, and idempotency controls. Slack and email
   summaries link to the authorized in-app snapshot when full content is not policy-safe.
9. **Preserve an immutable snapshot.** Core stores the exact approved and distributed
   version, evidence manifest, warnings, recipients, channel render/hash, delivery
   attempts and outcomes, approval audit, and timestamps. Corrections create a new
   superseding snapshot; they never mutate the distributed snapshot.

### 7.2 State model

`SCHEDULED -> DRAFTING -> AWAITING_INPUT -> READY_FOR_REVIEW -> AWAITING_APPROVAL ->
APPROVED -> DISTRIBUTING -> DISTRIBUTED`

Alternative states are `DEGRADED`, `DELIVERY_PARTIAL`, `DELIVERY_FAILED`, `REJECTED`,
`CANCELLED`, and `SUPERSEDED`. `DEGRADED` is a visible condition that can coexist with a
draft/review state; it is not permission to skip evidence or approval. State transitions,
actor, reason, prior version, and timestamp are audited.

### 7.3 Update-request behavior

- A request is deduplicated by report version, gap, responsible owner, and channel.
- Acknowledgement suppresses repeat reminders until the promised response time or policy
  threshold passes.
- Delegation transfers future requests and records both parties; the original owner is
  not repeatedly contacted unless delegation expires or fails according to policy.
- Responses are attributed, time-stamped, permission-checked, and classified as recorded
  input or owner interpretation before use.

## 8. Exception and degraded behavior

| Condition                                 | Required behavior                                                                                                                                                                                                                                                                                      |
| ----------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Incomplete report at send time            | Do not send automatically unless an administrator-configured policy explicitly permits the template, confidentiality level, channel, and gap severity. When permitted, show a prominent incomplete-report warning and enumerate gaps in every channel rendering.                                       |
| Metric unavailable                        | Display **Unavailable** with reason, affected period/source, and last known timestamp when safe. Never render unavailable or unprojected metrics as `0`, and never calculate a change from an unavailable value.                                                                                       |
| Nova unavailable                          | Preserve verified facts and evidence, mark generated sections unavailable or retain the last draft text as stale, and show a degraded-state warning. Do not imply deterministic fallback prose is current AI interpretation. The report remains reviewable but cannot silently lose required sections. |
| Relay unavailable or stale                | Show a degraded-state warning, mark affected KPIs/context unavailable or stale, retain timestamps, and block automatic incomplete delivery unless policy permits it. Core entity evidence may remain usable.                                                                                           |
| Owner acknowledged or delegated an update | Suppress duplicate update requests as described in section 7.3; expose acknowledgement/delegation status to the report owner.                                                                                                                                                                          |
| Approval missed                           | Escalate only when the configured policy specifies a delay, target, allowed channel, and maximum frequency. Otherwise leave the report awaiting approval and notify only through normal reminders. Never auto-approve.                                                                                 |
| Delivery partially fails                  | Preserve the approved snapshot, retry idempotently according to channel policy, show per-recipient/channel outcomes, and never resend successful deliveries as part of a broad retry.                                                                                                                  |
| Permission or confidentiality conflict    | Exclude unauthorized evidence, mark the resulting gap, block delivery to incompatible recipients, and require re-review after recipient or content correction. Never redact silently after approval.                                                                                                   |

## 9. Channel and confidentiality policy

The supported confidentiality levels are `Internal`, `Confidential`, `Restricted`, and
`External Confidential`. A report's effective level is at least the highest classification
of its included evidence and may be raised manually. It cannot be manually lowered.

- **In-app:** canonical review and snapshot experience; shows full provenance subject to
  current authorization and a safe captured fact when live evidence is no longer visible.
- **Email:** uses the existing email adapter and recipient verification. Restricted
  reports default to a minimal notification with an authenticated link unless policy
  explicitly permits encrypted/full-content delivery.
- **Slack:** uses existing Slack connection and notification preferences. Delivery is
  restricted to mapped users or approved private channels; content is minimized for
  Confidential, Restricted, and External Confidential reports.

Distribution is idempotent per snapshot, recipient, channel, and rendering version.
Unsubscribe/channel preferences apply unless a legally or operationally mandatory policy
explicitly overrides them. Delivery logs must avoid storing secrets or unnecessary report
content.

## 10. Functional requirements and acceptance criteria

### Configuration and generation

- A report owner can create, pause, resume, and retire a schedule using one scope and one
  governed template, with cadence, time zone, approver(s), channels, recipients,
  confidentiality, freshness thresholds, incomplete-send policy, reminders, and
  escalation policy.
- Invalid scope/template combinations, unauthorized recipients, unsupported channels,
  missing approvers, and confidentiality conflicts are rejected before activation.
- A scheduled occurrence is idempotent and creates at most one active draft for its
  configuration and effective period.
- Drafts reuse existing weekly-review, project-brief, dashboard/report, notification,
  email, and Slack concepts and adapters; no frontend-to-Relay or frontend-to-Nova path is
  introduced.

### Review, evidence, and approval

- All shared required content appears, including explicit unavailable/unknown states.
- Every distributed quantitative and qualitative claim satisfies section 6 evidence
  rules, or is visibly marked unresolved under an allowed incomplete-report policy.
- Fact and generated interpretation labels survive all supported channel renderings and
  exports.
- Reviewers can inspect material diffs, edit inline, regenerate one section, and see all
  unresolved freshness/evidence/input gaps before approval.
- Approval is bound to one version and is invalidated by configured material changes.

### Delivery and audit

- In-app, email, and Slack delivery use current Core notification/channel infrastructure,
  record per-recipient outcomes, and retry idempotently.
- Automatic delivery never bypasses completeness, approval, permission, or confidentiality
  policy.
- Every successful distribution produces an immutable snapshot; correction and recall
  records reference rather than alter the original.
- Audit history covers configuration, generation, evidence, input requests/responses,
  refreshes, edits, regeneration, approval/rejection, delivery, retries, escalation, and
  supersession.

## 11. Pilot and measurement plan

The pilot establishes a baseline before activation and compares like-for-like report
occurrences by template, scope, cadence, and participating manager. Results must disclose
sample size, unavailable telemetry, and material policy or population changes.

| Target                                                             | Definition and measurement                                                                                                                                                                                                                                                          |
| ------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| At least 60% reduction in report preparation time                  | Compare median active human minutes from first preparation action through approval-ready draft against the agreed baseline. Exclude passive waiting and measure follow-up time separately; report edited and incomplete reports as cohorts.                                         |
| At least 75% of generated sections accepted without material edits | Number of generated required sections approved with no material semantic, factual, evidence, or structural edit divided by generated required sections presented for review. Formatting and spelling-only edits are non-material.                                                   |
| At least 95% delivery success                                      | Successfully delivered recipient-channel attempts within the configured delivery SLO divided by valid intended recipient-channel attempts. Exclude pre-delivery policy blocks from the numerator and denominator but report them separately; include exhausted retries as failures. |
| Fewer than 2% of distributed factual claims reported as incorrect  | Unique distributed recorded-fact claims upheld as factually incorrect after review divided by unique distributed recorded-fact claims. Generated interpretations and freshness complaints are separate measures; corrections remain linked to snapshots.                            |
| At least 70% weekly use among participating managers               | Managers who generate, review, edit, approve, or distribute at least one report in a pilot week divided by managers enrolled and eligible for that full week. Report median across eligible pilot weeks and raw weekly cohorts.                                                     |

Pilot telemetry must additionally track time spent requesting updates, stale/missing input
rate, unsupported-claim block rate, approval latency, report incompleteness, Nova/Relay
degraded occurrences, delivery retries, section regeneration, edit distance, recipient
engagement where privacy policy permits it, and reported-incorrect-claim resolution time.

## 12. Ownership and architecture constraints

- **Core:** owns report configuration and schedules, authorization, source orchestration,
  gap/request workflow, draft/version lifecycle, approvals, immutable snapshots, channel
  distribution, and audit records. Core exposes all report APIs to the frontend.
- **Relay:** owns metric projections, trends, dashboard/report rollups, project/team/
  portfolio read context, availability, and source freshness. Relay metrics remain the
  required source for aggregate quantitative claims.
- **Nova:** owns template prompts, synthesis, interpretation labels, and AI-run audit. It
  receives only authorized minimized context and returns structured sections with evidence
  candidates; it does not approve, distribute, or mutate Core work.
- **Frontend:** extends existing weekly-review, project-brief, reports/dashboard, and
  notification surfaces with schedule, gap, review, approval, and snapshot views. It calls
  Core only.
- **Email and Slack:** remain delivery adapters behind Core notification/integration
  policy, not independent report stores or sources of truth.

Any later implementation must follow the repository's service boundaries, keep Core
OpenAPI synchronized, add Flyway-owned persistence with optimistic concurrency where
mutable, and treat snapshots and audit artifacts according to configured retention,
legal-hold, residency, and deletion policies.

## 13. Open decisions before implementation

1. Exact material-change classification and whether any spelling-only post-approval edit
   is permitted without reapproval.
2. Workspace defaults for freshness thresholds by source, template, and incident cadence.
3. Co-approval and separation-of-duties rules for customer and Restricted reports.
4. Storage/rendering policy for immutable snapshot evidence excerpts after a user loses
   access or a source entity is deleted.
5. Which Core entity will represent first-class decisions and risks where current task or
   activity records are insufficient.
6. Allowed Slack/email content by confidentiality level and customer residency policy.
7. Pilot definition of a valid factual-incorrectness report, adjudicator, and dispute
   window.
