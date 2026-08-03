# Objectives and Execution Alignment Product Specification

## 1. Purpose and product fit

TaskMind must connect strategy to delivery without implying that activity is the same as
outcome. Leaders define objectives and measurable key results, managers align planned
work, and teams continue to own and execute projects and tasks through their existing
workflows. The resulting control tower explains whether outcomes are improving, where
capacity is invested, which commitments are at risk, and which executive decisions are
needed.

This capability extends TaskMind's project, milestone, task, analytics, reporting, and AI
experiences. It is not a replacement for project planning, financial planning, or a
general-purpose business-intelligence platform.

### Goals

1. Establish a consistent, auditable hierarchy from organizational outcomes to execution.
2. Make strategic coverage, unsupported outcomes, and unaligned capacity visible without
   forcing every item of work into an objective.
3. Reduce the effort required to prepare trustworthy executive status while keeping facts,
   measurements, and AI interpretation distinct.
4. Help leaders improve plans through explainable AI recommendations that never make
   strategic, ownership, or target changes autonomously.

### Non-goals

- Automatically assigning objectives, projects, milestones, or tasks to different owners.
- Treating completed tasks, story points, or spend as proof that an outcome improved.
- Replacing source systems that author business measurements.
- Allowing AI-generated narrative to overwrite a measured key-result value.
- Requiring operational, regulatory, maintenance, or exploratory work to claim a strategic
  relationship it does not have.
- Building portfolio budgeting, compensation scoring, or individual performance ranking in
  the first release.

## 2. Users and jobs to be done

| Persona                      | Primary job                                                                                                          |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| Executive                    | Understand outcome progress, confidence, investment, risks, and decisions across an authorized organizational scope. |
| Strategy administrator       | Govern objective cycles, organizational scopes, permissions, units, and target-change policy.                        |
| Objective owner              | Define an outcome, approve its key results, review measurements, explain confidence, and request corrective action.  |
| Key-result owner             | Maintain the measurement contract, validate current values, and assess whether planned work is sufficient.           |
| Portfolio or program manager | Align initiatives and projects, expose dependencies, balance capacity, and identify unsupported or duplicated work.  |
| Project manager              | Declare how a project supports one or more key results without changing delivery ownership.                          |
| Team member                  | See the inherited strategic context of assigned work while retaining the task's operational meaning and ownership.   |
| Auditor or analyst           | Reconstruct historical targets, measurements, alignments, approvals, and the evidence behind status reporting.       |

## 3. Strategic hierarchy and semantics

The canonical hierarchy is:

`Objective -> Key result -> Initiative -> Project -> Milestone -> Task`

The hierarchy describes containment and planning granularity. Strategic support is an
explicit relationship rather than an assumption derived from containment.

| Level          | Definition                                                                                                                                                                                  | Required relationship behavior                                                                                                                                       |
| -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Objective**  | A qualitative, time-bound outcome that expresses what an organizational scope intends to achieve. It should be directional, motivating, and specific enough to govern key-result selection. | Belongs to one objective cycle and organizational scope; contains one or more key results.                                                                           |
| **Key result** | A quantitative, time-bound measure proving whether the objective's intended outcome occurred.                                                                                               | Belongs to exactly one objective; receives versioned measurements; may be supported by zero or more initiatives and projects.                                        |
| **Initiative** | A coordinated strategic bet or program intended to influence one or more key results, potentially spanning multiple projects.                                                               | May support multiple key results through explicit alignment records; may contain or group projects.                                                                  |
| **Project**    | A governed body of delivery work with an owner, scope, dates, capacity, and outcome hypothesis.                                                                                             | May support multiple key results with explicit weights; may be categorized as operational or left unaligned.                                                         |
| **Milestone**  | A significant checkpoint or deliverable within a project.                                                                                                                                   | Inherits alignment visibility from its project for context; does not create additional strategic value unless separately and explicitly aligned in a future release. |
| **Task**       | The smallest assignable execution item.                                                                                                                                                     | Inherits alignment visibility through its project and milestone, but never automatically inherits or contributes strategic value.                                    |

### 3.1 Terminology rules

- **Containment** answers “where is this work organized?”; **alignment** answers “which
  outcome is this work expected to influence?”
- “Supports” means there is a versioned, user-confirmed alignment record. It does not mean
  the work caused the measured result.
- “Unaligned” means a project has no active strategic alignment and no active operational
  category. It is a valid, visible state rather than a validation error.
- “Operational work” means committed capacity intentionally categorized as business-as-usual,
  maintenance, regulatory, incident, service, or another governed non-strategic category.
- Archived objectives and their descendants are read-only for ordinary users and remain
  queryable in historical reports, comparisons, snapshots, and audits.

## 4. Objective and key-result data contract

### 4.1 Shared properties

Every objective and key result must expose the following properties. Values are versioned
where indicated so reports can be reconstructed as of a past date.

| Property                            | Objective behavior                                                                                                                                      | Key-result behavior                                                                                                                                                                                               |
| ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Owner**                           | Exactly one accountable user; delegates and collaborators may be recorded separately.                                                                   | Exactly one accountable user, who may differ from the objective owner.                                                                                                                                            |
| **Organizational scope**            | Exactly one authorized company, business unit, department, team, or other governed organization node; child-scope visibility follows policy.            | Inherits the objective scope by default; narrowing to a child scope is allowed, widening is not.                                                                                                                  |
| **Measurement unit**                | Optional summary unit when all key results are comparable; otherwise “mixed.”                                                                           | Required unit definition, such as percent, currency plus ISO currency code, count, duration, score, ratio, or a workspace-defined unit. Directionality (`increase`, `decrease`, or `maintain range`) is required. |
| **Baseline and target**             | Derived rollup only; no fabricated arithmetic across incomparable key results.                                                                          | Required numeric baseline and target, with precision and optional lower/upper bounds for maintain-range results. Material target edits are versioned and may require approval.                                    |
| **Current value**                   | Computed progress summary from eligible child key results, never an editable business measurement.                                                      | Latest accepted measured value with effective time, observed time, source reference, freshness state, and verification state.                                                                                     |
| **Start and target dates**          | Required and define the objective period.                                                                                                               | Required; must fall within the objective period unless an authorized exception with rationale is approved.                                                                                                        |
| **Confidence**                      | Required forecast of achieving the objective by target date, represented as a calibrated percentage plus `high`, `medium`, or `low` band and rationale. | Required forecast of achieving the target by its date, distinct from progress and status.                                                                                                                         |
| **Status**                          | `DRAFT`, `PENDING_APPROVAL`, `ACTIVE`, `AT_RISK`, `OFF_TRACK`, `ACHIEVED`, `CANCELLED`, or `ARCHIVED`.                                                  | `DRAFT`, `NOT_STARTED`, `ON_TRACK`, `AT_RISK`, `OFF_TRACK`, `ACHIEVED`, `MISSED`, `CANCELLED`, or `ARCHIVED`.                                                                                                     |
| **Update cadence**                  | Required review cadence: weekly, biweekly, monthly, quarterly, or a governed custom schedule, with time zone and next-due date.                         | Required measurement/review cadence; may be more frequent than the objective cadence but not silently less frequent.                                                                                              |
| **Linked initiatives and projects** | Read-only rollup of active alignment records across child key results, deduplicated and permission-filtered.                                            | Explicit, versioned alignments with relationship type, weight where applicable, rationale, actor, effective dates, and state.                                                                                     |
| **Source of current measurement**   | Rollup of child evidence coverage and freshness, without implying one source when several exist.                                                        | Required source descriptor including source type, stable source identifier, metric or field, aggregation/filter context, source-system timestamp, ingestion timestamp, and optional verification actor.           |

Objective and key-result records additionally require a stable ID, title, description,
created/updated metadata, version, objective cycle, and applicable confidentiality label.
Descriptions explain intent but never substitute for the structured measurement contract.

### 4.2 Progress calculation

For an increasing key result, normalized progress is
`(current - baseline) / (target - baseline)`. For a decreasing key result, it is
`(baseline - current) / (baseline - target)`. Display progress is capped to the configured
visual range, while the raw value and overachievement remain available. Maintain-range
results are evaluated against their approved lower and upper bounds and cannot use the
increase/decrease formula.

The UI must explicitly handle an equal baseline and target, missing values, non-numeric
sources, stale values, and reversed direction. It must show `unavailable` rather than
divide by zero, infer a zero, or silently coerce invalid data.

Objective progress is the weighted average of eligible key-result normalized progress.
Key-result contribution weights must total 100% within an active objective. If weights are
not configured, equal weighting is used and labeled. Missing or stale key results remain
visible and reduce evidence completeness; workspace policy determines whether they are
excluded from arithmetic or retain their last accepted value, and the chosen policy must
be displayed with the rollup.

### 4.3 Confidence, status, and freshness

- Progress reports observed movement; confidence forecasts target attainment; status
  communicates management attention. The three values must never be conflated.
- A human owner can set confidence and rationale. AI may propose confidence with supporting
  factors, but a proposal remains labeled until accepted.
- Status can be suggested by deterministic thresholds or AI, but transitions with material
  governance impact require an authorized user.
- Freshness is `FRESH`, `DUE_SOON`, `STALE`, `MISSING`, or `SOURCE_ERROR`, calculated from
  the key result's cadence, expected arrival time, and source state.
- Every displayed rollup includes its as-of time and the count of fresh, stale, missing,
  and inaccessible child measurements.

## 5. Alignment model and behavior

### 5.1 Explicit alignment record

An alignment is a first-class, versioned record containing the key result, supporting
initiative or project, relationship type (`DIRECT`, `ENABLING`, or `CONTRIBUTING`), weight,
rationale, confidence, creator, approval state, effective interval, and audit metadata.
Project-to-key-result alignments additionally include the project's planned capacity for
the reporting period and an optional outcome hypothesis.

1. A project may support multiple key results. Its active key-result weights must each be
   greater than 0% and must total no more than 100% for the same capacity period.
2. A weight allocates the project's committed capacity for portfolio analysis; it is not
   a claim that the project contributes that percentage of outcome progress or causality.
3. The unused portion is displayed as unallocated project capacity unless it is assigned
   to a governed operational-work category.
4. Initiative alignment does not automatically align every contained project. The
   initiative view distinguishes explicitly aligned projects from projects that are only
   in an aligned initiative.
5. A task or milestone displays the nearest active project alignments as inherited
   context. Its completion, estimate, or assignee does not automatically add strategic
   value, change a key-result measurement, or create an alignment record.
6. Work may be saved and executed without a strategic link. Projects with neither an
   active alignment nor an operational category appear in the unaligned-work view.
7. Changing, adding, expiring, rejecting, or removing an alignment never changes the
   owner or assignee of an objective, key result, initiative, project, milestone, or task.
8. Archiving an objective expires active reporting relationships at the archive effective
   time but preserves every historical alignment and report snapshot. It does not archive
   linked work.

### 5.2 Alignment lifecycle

`PROPOSED -> CONFIRMED -> ACTIVE -> EXPIRED`

Alternative states are `REJECTED` and `REMOVED`. AI-created recommendations enter
`PROPOSED`; they have no portfolio effect until an authorized human confirms them. A
material alignment or weight change creates a new version rather than overwriting the
prior record. Backdated changes require elevated permission and a rationale.

### 5.3 Coverage and capacity metrics

- **Committed project capacity** is the planning-system capacity assigned to committed
  projects in the selected period, expressed in a single configured portfolio unit such
  as person-days, hours, or cost. Mixed units are not summed without an approved conversion.
- **Strategically mapped capacity** is committed capacity multiplied by active confirmed
  alignment weights, deduplicated per project and period.
- **Operational capacity** is committed capacity explicitly assigned to governed
  operational categories.
- **Coverage** is `(strategically mapped capacity + operational capacity) / committed
project capacity`. The dashboard shows both components so operational categorization
  cannot masquerade as strategic alignment.
- Projects with unavailable capacity remain in item counts and evidence warnings but are
  excluded from the capacity denominator with an explicit completeness indicator.

## 6. Core user workflows

### 6.1 Plan an objective cycle

1. An authorized strategy user creates a draft objective with scope, owner, dates,
   cadence, and confidentiality.
2. The owner creates key results or asks AI to draft candidates.
3. Each accepted key result receives an owner, measurement unit, direction, baseline,
   target, dates, cadence, source contract, and contribution weight.
4. Validation flags activity metrics presented as outcomes, missing sources, incompatible
   dates, equal baselines and targets, and ambiguous units.
5. Required approvers review the objective and any material targets before activation.
6. Activation records an immutable initial target snapshot and begins update reminders.

### 6.2 Align execution

1. A portfolio or project manager opens a key result or project and reviews existing and
   AI-recommended relationships.
2. The user selects one or more key results, enters a relationship type, rationale, and
   explicit capacity weight for each relationship.
3. TaskMind validates scope access, date overlap, duplicate relationships, and the 100%
   project weight ceiling.
4. Confirmation activates the alignment without changing any ownership.
5. Project, milestone, and task views show inherited alignment context; portfolio views
   update coverage and investment after projection freshness is confirmed.

### 6.3 Update and review outcomes

1. Scheduled or manual ingestion records a candidate key-result measurement with source
   provenance and effective time.
2. Source and policy rules determine whether it is automatically accepted or requires
   human verification. Corrections append a superseding value.
3. TaskMind recomputes progress, freshness, and deterministic risk signals.
4. Owners review confidence, explain changes, and accept or reject any AI narrative.
5. Executives consume a permission-filtered control-tower snapshot and record decisions
   or corrective actions without altering source measurements.

## 7. AI-assisted workflows

Nova may assist with the workflows below using only Core-authorized context and
Relay-provided analytics. Every recommendation must include a concise rationale,
confidence score and band, supporting evidence references, generation time, and model or
prompt-version audit metadata. Recommendations never take effect until accepted by an
authorized human.

| Workflow                                   | Required inputs                                                                                                                                       | Output and guardrails                                                                                                                                                                                                                               |
| ------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Draft measurable key results**           | Objective intent, scope, dates, approved vocabulary, available metric catalog, and authorized historical context.                                     | Propose outcome-oriented key results with unit, direction, baseline/source gaps, target, dates, cadence, owner role suggestion, and rationale. Never invent a baseline or claim that an unavailable source exists.                                  |
| **Recommend existing supporting projects** | Key-result definition, authorized project metadata, timing, scope, descriptions, dependencies, and capacity.                                          | Rank candidate projects; cite semantic and structured evidence; propose relationship type and weight range. Clearly distinguish existing explicit links from similarity.                                                                            |
| **Detect duplicated initiatives**          | Authorized initiative titles, descriptions, outcomes, scopes, dates, owners, projects, and dependencies.                                              | Return candidate duplicate clusters with overlap dimensions, confidence, evidence, and options to merge, clarify, or retain. Never merge automatically.                                                                                             |
| **Identify insufficient planned work**     | Key-result trajectory, remaining gap and time, confirmed alignments, weighted capacity, dependencies, risks, and historical delivery signals.         | Flag unsupported or under-supported key results; explain planning gaps and data limitations. Do not represent planned work as guaranteed outcome movement.                                                                                          |
| **Detect material unaligned capacity**     | Current objective cycle, authorized committed projects, capacity, operational categories, and alignment history.                                      | Rank projects consuming significant capacity without a current strategic link or operational category; show threshold, period, amount, and likely candidate links.                                                                                  |
| **Generate an executive narrative**        | Permission-filtered objective snapshots, measurements, trends, confidence history, alignment coverage, risks, dependencies, freshness, and decisions. | Produce a labeled AI narrative separating recorded facts, owner commentary, inference, uncertainty, and required decisions. Every quantitative claim must cite a source measurement or portfolio calculation.                                       |
| **Propose corrective actions**             | Underperforming objective context, owner-recorded causes, capacity, dependencies, risks, and prior actions.                                           | Suggest options such as re-sequencing, adding support, resolving a dependency, revising scope, or initiating target governance. Include expected mechanism, trade-offs, responsible role, urgency, confidence, and evidence; never execute changes. |

### 7.1 AI review and feedback

- Users can accept, edit, reject, or defer each recommendation independently and provide a
  structured reason.
- Edited output remains labeled as AI-assisted and records the human editor; accepted
  alignment data is thereafter a human-confirmed business record.
- Low-confidence or poorly grounded recommendations are suppressed or placed in a review
  queue according to workspace policy.
- The system measures acceptance, rejection reason, reversal, and downstream usefulness
  without using restricted content for unauthorized training or evaluation.
- Prompt injection defenses treat project text and source content as untrusted data, never
  as system instructions.

## 8. Control-tower experience

The control tower defaults to the current objective cycle and the viewer's authorized
organizational scope. Every card supports drill-down to the governing record and evidence.
Filters include cycle, as-of date, organization, owner, objective status, confidence,
freshness, initiative, project, and operational category. Historical mode reconstructs
the selected time rather than applying today's alignments to old results.

| Area                                     | Minimum experience                                                                                                                                                                                                                   |
| ---------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Objective progress and confidence**    | Show normalized progress, raw key-result values, target trajectory, confidence band and trend, status, owner, target date, update due state, and evidence completeness. Distinguish progress from confidence visually and textually. |
| **Strategic alignment coverage**         | Show capacity coverage, item-count coverage, confirmed versus proposed alignment, operational categorization, unallocated weight, and projects with unknown capacity.                                                                |
| **Investment or capacity by objective**  | Allocate project capacity using confirmed explicit weights; show unit, period, total, trend, operational share, and double-counting safeguards. Do not imply ROI or causality.                                                       |
| **At-risk initiatives and dependencies** | Rank initiatives and cross-project dependencies by affected key results, timing, severity, confidence, and capacity exposure; expose owner and next action.                                                                          |
| **Unaligned work**                       | List projects with no current strategic link or operational category, their owner, status, committed capacity, age, and any explainable AI candidate links.                                                                          |
| **Outcome trends**                       | Plot source measurements, baseline, current value, target, target trajectory, forecast range where enabled, accepted corrections, and stale or missing intervals.                                                                    |
| **Required executive decisions**         | Present decision, accountable decision-maker, due date, affected objectives/key results, options or recommendation, cost of delay, evidence, and decision state.                                                                     |
| **Evidence and data freshness**          | Show source, metric/field, as-of time, ingestion time, cadence, freshness state, verification state, inaccessible evidence, and last successful refresh. Report-wide freshness never hides stale child sources.                      |

### 8.1 Alerts and attention rules

The experience highlights, but does not silently change, objectives when measurements are
stale, confidence falls materially, target trajectory is missed, aligned capacity declines,
a critical dependency threatens multiple key results, or an executive decision passes its
due date. Users can acknowledge, assign, snooze within policy, or resolve alerts. Alert
thresholds and acknowledgements are auditable.

## 9. Governance, authorization, and audit

### 9.1 Roles and permissions

- Workspace administrators configure objective cycles, organization scopes, units,
  operational categories, materiality thresholds, approval policy, and role grants.
- Only users with `objective:create` may create an objective; only scoped users with
  `objective:change` may change its definition, owner, dates, status, or key-result set.
- Key-result owners may submit measurements and confidence updates within scope but cannot
  bypass objective approval or material target-change policy.
- Project managers may propose or maintain alignments for projects they can administer;
  cross-scope relationships require authorization to both sides or a governed approval.
- View, export, narrative generation, and evidence resolution re-check authorization at
  request time. Possession of a report link or historical snapshot does not grant access.

### 9.2 Material target changes

A material target change includes changing the baseline, target, unit, direction, target
date beyond the configured tolerance, maintain-range bounds, or measurement source in a
way that breaks comparability. Workspace policy may also define numeric thresholds.

1. An authorized editor submits the proposed value, reason, expected impact, and effective
   date.
2. The existing target remains active while approval is pending.
3. A configured approver who is not the requester approves or rejects the exact change.
4. Approval appends a target version and triggers recalculation from its effective date;
   prior snapshots retain the prior target.
5. Reports show original, current, and as-of target and visibly mark restatements so a
   revised target cannot erase an earlier miss.

### 9.3 History and auditability

The audit log preserves actor, action, timestamp, reason, previous and new values, effective
time, approval, request correlation, and source for objective, key-result, target,
measurement, confidence, status, alignment, weight, operational category, and archive
changes. Measurement corrections and alignment removals are append-only supersessions.
Historical reports bind their objective versions, target versions, measurement IDs,
alignment versions, capacity inputs, freshness state, and narrative version.

### 9.4 Facts, measurements, and AI narrative

- Measured key-result values live in a structured measurement ledger. Only approved source
  ingestion or authorized human correction can append or supersede them.
- Owner commentary and AI narrative live in separate, labeled fields and versions. Neither
  can mutate a measurement, target, confidence, alignment, or status implicitly.
- AI text must distinguish recorded fact, owner assertion, inference, recommendation, and
  unavailable evidence. Quantitative statements link to immutable measurement or rollup
  references.
- Regeneration creates a new narrative version and does not overwrite accepted human notes.

### 9.5 Cross-project confidentiality

Before retrieval, Core resolves the viewer's and service principal's allowed organization,
project, field, and confidentiality scopes. Relay aggregates only authorized projections,
and Nova receives the minimum permission-filtered context required for the request.

Summaries must not reveal the existence, title, owner, content, metric, capacity, or inferred
status of an unauthorized project. Cross-project aggregates must follow configured minimum
cohort and suppression rules. Citations are permission-checked again when opened; evidence
that became inaccessible is labeled unavailable without leaking its contents. Cached
context, embeddings, prompts, outputs, exports, and logs retain tenant and authorization
boundaries and are invalidated when access changes.

## 10. Reliability and edge cases

- Measurement ingestion is idempotent by source, metric, effective time, and source
  revision; repeated delivery must not double-count or create false trends.
- Capacity aggregation is idempotent by project, planning period, capacity unit, and plan
  version. Overlapping plans require explicit precedence.
- Concurrent objective, target, and alignment edits use optimistic locking and return a
  resolvable conflict rather than last-write-wins data loss.
- Time periods use inclusive start and exclusive end instants plus an organizational time
  zone. Date boundaries are preserved in historical reports.
- Source outages retain the last accepted measurement with a stale label and incident
  state; they never substitute zero or allow AI to estimate a recorded value.
- Deleted or inaccessible owners appear as unresolved references until reassigned; records
  and history remain intact.
- Currency and unlike capacity units are not combined without dated, governed conversion
  rules. Converted values retain original value, unit, rate source, and rate date.
- Archiving is reversible only through an authorized restore workflow; restore does not
  silently reactivate expired alignments or old update schedules.

## 11. Notifications and operating cadence

TaskMind notifies the responsible user when a key-result update is due or stale, a target
change awaits approval, an alignment proposal awaits review, confidence materially drops,
a critical dependency changes, or an executive decision is due. Notifications respect
preferences, scope, confidentiality, deduplication, and escalation policy. Acknowledgement
does not mark the underlying measurement or decision complete.

Objective-cycle reviews produce immutable, permission-filtered snapshots at the configured
cadence. Executives may compare snapshots without recomputing old periods using current
targets or alignments.

## 12. Success metrics and acceptance criteria

### 12.1 Product success metrics

| Success criterion                                                                                                       | Operational definition                                                                                                                                                                                                                                                   |
| ----------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **At least 80% of committed project capacity is mapped to an objective or explicitly categorized as operational work.** | For pilot workspaces, median monthly coverage reaches at least 80% by the end of the agreed adoption period, using the coverage formula in section 5.3. Both strategically mapped and operational shares are reported. Unknown-capacity projects are separately visible. |
| **Managers can identify unsupported key results without manually reviewing project plans.**                             | In a moderated validation, at least 90% of participating managers correctly find all seeded key results with no or insufficient confirmed planned work from the control tower in five minutes or less, without opening individual project plans.                         |
| **Executive status preparation time falls by at least 50%.**                                                            | Median active preparation time for the same recurring executive review, measured before rollout and after four consecutive production cycles, declines by at least 50% without a material decline in factual-correction rate.                                            |
| **Every AI-generated alignment recommendation includes rationale and confidence.**                                      | 100% of displayed, exported, or API-returned recommendations contain non-empty rationale, numeric confidence, confidence band, and evidence or an explicit insufficient-evidence state; invalid outputs are blocked from display.                                        |
| **Objective progress remains traceable to source measurements.**                                                        | 100% of displayed current values and objective rollups resolve to versioned measurement records with source identity, effective/as-of time, freshness, and target version; inaccessible evidence is identified without disclosure.                                       |

### 12.2 Functional acceptance criteria

1. Authorized users can create, approve, activate, complete, cancel, archive, and
   historically view objectives and key results with all properties in section 4.
2. A project can carry multiple key-result alignments whose active weights are validated,
   historized, and used for capacity reporting without changing ownership.
3. Milestone and task views show inherited alignment context and clearly state that task
   completion does not update outcome value.
4. Users can save unaligned work, categorize operational work, and find both states in the
   control tower.
5. Archived objectives, prior targets, measurements, and alignments remain available in
   as-of reporting.
6. Material target changes require configured approval and cannot rewrite historical
   snapshots.
7. Each AI workflow in section 7 supports human review and supplies rationale, confidence,
   evidence, and an explicit no-action-without-confirmation boundary.
8. Executive narrative renders measured facts, human commentary, and AI interpretation as
   separate, traceable content types.
9. The control tower provides every area in section 8 with freshness and evidence state,
   including incomplete and inaccessible-data behavior.
10. Authorization tests prove that summaries, recommendations, aggregates, searches,
    citations, and exports do not expose unauthorized cross-project context.

## 13. Analytics and evaluation instrumentation

TaskMind records privacy-safe events for objective activation, target-change submission and
approval, measurement freshness, alignment proposal and confirmation, recommendation
acceptance or rejection, coverage calculation, unsupported-key-result detection, control-
tower drill-down, narrative generation and correction, and executive decision resolution.

Evaluation dashboards track coverage and its completeness, stale-source rate, unsupported
key-result precision/recall against manager review, recommendation acceptance and reversal,
narrative quantitative-claim traceability, preparation time, factual-correction rate, and
authorization/suppression failures. Individual performance scoring is prohibited.

## 14. Architectural ownership for future implementation

- **Core** owns objective and key-result workflow, authorization, target and alignment
  records, approvals, operational categories, measurement acceptance, history, notifications,
  and the frontend facade APIs.
- **Relay** owns authorized analytics projections, progress and coverage rollups, trends,
  freshness/read context, capacity analysis, and historical control-tower query models.
- **Nova** owns prompts and model calls for drafting, matching, duplicate detection,
  sufficiency analysis, unaligned-capacity interpretation, executive narrative, and
  corrective-action proposals.
- **Frontend** calls only Core and renders hierarchy, alignment review, governance,
  control-tower, evidence, and AI labels from typed Core contracts.
- **Events and AI contracts** carry stable IDs, tenant and authorization context, versions,
  effective/as-of times, source references, and idempotency keys. They must not turn
  inferred narrative into a domain measurement.

## 15. First-release boundaries and open decisions

The first release should support one active objective cycle per organizational scope,
numeric key results, explicit project alignment weights, governed operational categories,
historical reporting, the control tower, and reviewable AI assistance. It should not
automatically optimize resource allocation or infer causal impact.

The following decisions require product validation before implementation:

1. Which capacity unit and source is authoritative for each pilot workspace, and how often
   is the committed plan version frozen?
2. What numeric and date tolerances make a target change material for each organization?
3. Should stale key results be excluded from objective arithmetic or retain their last
   accepted value, and how prominently must each policy affect confidence?
4. What planning threshold defines “insufficient” support without implying that more
   activity guarantees an outcome?
5. Which minimum cohort sizes and suppression policies apply to cross-project aggregates?
6. Which objective and key-result contribution weighting method is easiest for pilot users
   to understand and govern?
7. Which source connectors can provide verifiable current values in the pilot, and which
   measurements will require controlled manual entry?
