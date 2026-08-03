# Cross-Team Recovery Planning Product Requirements Document

| Metadata | Value                                                                                               |
| -------- | --------------------------------------------------------------------------------------------------- |
| Status   | Proposed for discovery and pilot validation                                                         |
| Owner    | Product                                                                                             |
| Audience | Product, design, engineering, security, operations, and pilot stakeholders                          |
| Scope    | Product requirements and safety constraints; this document does not define implementation contracts |

## 1. Purpose

Cross-team recovery planning helps a manager respond when a milestone is unlikely to meet
its current commitment. TaskMind must turn current plan, capacity, dependency, access,
and policy data into two to four feasible, meaningfully different recovery scenarios.
The manager can compare, consult affected owners, edit, explicitly approve, and apply one
scenario without losing the original plan or its decision history.

The feature is decision support, not autonomous planning. AI may propose and explain a
scenario, but deterministic services remain authoritative for dates, capacity,
dependencies, permissions, policy, and application. No generated proposal changes
business state before explicit approval.

## 2. Goals and non-goals

### Goals

- Shorten the time between detecting milestone risk and approving a coordinated response.
- Offer genuinely different recovery strategies rather than cosmetic variations of one
  plan.
- Make forecast, scope, people, capacity, dependency, effort, risk, reversibility, and
  assumptions comparable before a decision.
- Prevent infeasible, unauthorized, stale, or partially applied plan changes.
- Keep affected owners involved when consultation or approval policy requires it.
- Preserve an auditable original plan, proposal history, approvals, applications, and
  observed outcomes.
- Learn whether an approved recovery improved the forecast without presenting correlation
  as proof of causation.

### Non-goals

- Fully autonomous rescheduling, reassignment, scope removal, or commitment changes.
- Employee performance scoring or ranking people as interchangeable resources.
- Inventing skills, availability, costs, dependencies, or external-party commitments.
- Replacing portfolio governance, workforce planning, financial approval, or regulatory
  approval systems.
- Guaranteeing delivery merely because a scenario is feasible at validation time.
- Optimizing across work that the requesting user is not authorized to discover.

## 3. Users and responsibilities

| Role                                 | Responsibility                                                                                                                           |
| ------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------- |
| Initiating manager                   | Opens the high-risk milestone, compares scenarios, resolves warnings, edits a selected scenario, and requests final approval.            |
| Affected owner                       | Reviews proposed changes to their work, allocation, ownership, dates, or dependencies and responds when consultation policy requires it. |
| Milestone owner                      | Remains accountable for the forecast and confirms changes to milestone commitments when authorized.                                      |
| Approver                             | Approves or rejects the final change set under workspace, project, financial, regulatory, or external-commitment policy.                 |
| Workspace or portfolio administrator | Configures capacity, consultation, approval, assignment, external commitment, and rollback policies.                                     |
| Auditor or governance reviewer       | Inspects immutable proposal versions, validations, consultations, approvals, applications, rollbacks, and outcomes.                      |

One user may hold several roles, but the workflow must enforce configured separation of
duties. Access to a milestone does not imply permission to view or modify every linked
project, person's schedule, cost, or external dependency.

## 4. Entry conditions and source data

A recovery plan may be opened from a milestone classified as high risk by TaskMind or
manually flagged by an authorized manager. The workspace must define the risk threshold
or permit an authorized manual reason. The experience shows the risk trigger, current
forecast date and confidence, committed date, scope, incomplete data, and the timestamp
of the planning baseline.

Generation uses only permission-scoped, sufficiently fresh records, including:

- milestone scope, commitment, forecast, confidence, and success criteria;
- tasks, estimates, status, owners, candidate owners, and required skills;
- dependency graph, including internal and visible external dependencies;
- working calendars, working hours, approved leave, and configured allocation limits;
- project membership, access, and modification permissions;
- locked or externally managed work and its authoritative system;
- approval, regulatory, budget, and consultation policies; and
- cost or effort data when available and authorized.

Missing, stale, restricted, or contradictory inputs must be visible. TaskMind must not
infer that a person is available, qualified, or permitted merely because data is absent.

## 5. Supported scenario actions

A scenario may combine the actions below. Each action must identify its target, proposed
before-and-after values, rationale, required permission or approval, and validation
status.

### 5.1 Reorder dependent work

Change task sequence or planned dates while preserving valid dependency direction and
required lead or lag times. The proposal must show the affected critical path, downstream
date changes, and any newly exposed contention. It must never solve a delay by violating
a dependency.

### 5.2 Move noncritical work after the milestone

Move work that is not required for the milestone's agreed success criteria beyond the
milestone date. The proposal must explain why the work is noncritical, retain it in a
visible backlog or follow-on milestone, and show any downstream commitment or dependency
that moves with it. Moving work is not equivalent to deleting it.

### 5.3 Split a task

Divide a task into independently trackable work with explicit scope, estimates, owners,
dates, completion rules, and dependencies. The proposal must preserve traceability to the
source task, prevent duplicated scope, and state which part is required for the milestone.
A task may be split only when the work is meaningfully separable.

### 5.4 Reassign work to a qualified available person

Propose a different owner only when required skills, project access, working calendar,
leave, allocation, and configured eligibility rules validate. The current and proposed
owners must be visible, and consultation or acceptance must occur where policy requires.
TaskMind must never silently reassign a person or treat inferred skills as verified.

### 5.5 Reduce or defer scope

Remove an agreed portion from the milestone outcome or defer it to an explicit future
destination. The proposal must identify changed acceptance criteria, customer or
stakeholder impact, retained value, follow-up ownership, and required scope approval.
Deferred scope must remain traceable and cannot be presented as completed.

### 5.6 Add an approval checkpoint

Insert a named approval with an approver or eligible approver group, entry criteria, due
date, consequence of rejection or delay, and dependency relationships. The scenario must
include the checkpoint's expected delay and effort rather than treating governance as
zero-cost.

### 5.7 Change a milestone date

Propose a new forecast and, separately, a new committed milestone date. A forecast may be
updated as an estimate under policy; an internal or external commitment requires an
authorized confirmation. The proposal must show affected downstream milestones,
communications, contractual or customer implications, and required approvers.

### 5.8 Escalate an external dependency

Create a governed escalation proposal for a dependency controlled outside the planning
team. It must identify the external owner or escalation channel when known, requested
outcome, accountable internal owner, due date, fallback, communication sensitivity, and
evidence. Escalation does not claim that the external party accepted a new date or duty.

## 6. Required scenario contents

Every generated, edited, and final scenario must provide the following, including an
explicit **unknown**, **none**, or **not available to this viewer** state where applicable:

| Field                               | Requirement                                                                                                                                                                                 |
| ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| New forecast date and confidence    | Show the resulting forecast date, calibrated confidence representation, baseline delta, calculation timestamp, and material drivers. Confidence must not be conveyed by color alone.        |
| Scope affected                      | List included, deferred, reduced, split, or otherwise changed deliverables and acceptance criteria, plus unaffected critical scope.                                                         |
| People and teams affected           | Identify current and proposed owners, teams, approvers, consultees, and downstream stakeholders without exposing restricted personal data.                                                  |
| Capacity changes                    | Show allocation and effort changes by relevant period, remaining headroom, calendar basis, and any unavailable capacity.                                                                    |
| Dependency changes                  | Show added, removed, reordered, escalated, or date-shifted dependencies and resulting critical-path effects.                                                                                |
| Cost or effort implication          | Show estimate and currency or effort unit, baseline delta, data source, and uncertainty when available; otherwise state that it is unavailable rather than assuming zero.                   |
| New risks introduced                | Describe likelihood, impact, affected outcome, mitigation, owner, and trigger for each newly introduced or worsened risk.                                                                   |
| Reversible and irreversible actions | Classify every action; state the rollback operation and deadline for reversible actions, and the consequence and extra confirmation for irreversible actions.                               |
| Assumptions                         | List factual and planning assumptions, source, freshness, sensitivity to failure, and who must confirm material assumptions.                                                                |
| Why it may improve the outcome      | Explain the causal planning rationale, expected forecast or confidence effect, trade-offs, and why the scenario differs materially from alternatives; label model-generated interpretation. |

Each scenario also shows validation status, blocking violations, warnings, required
consultations and approvals, and the baseline version against which it was evaluated.
Scenario labels must describe the strategy, such as **protect scope by moving the date**
or **protect date by deferring scope**, rather than imply that one option is objectively
best.

## 7. Scenario diversity and generation behavior

TaskMind generates two to four scenarios when at least two feasible, meaningfully
different strategies exist. Difference must be material in one or more of scope,
ownership, sequencing, dependency treatment, milestone date, cost or effort, approval
path, or risk exposure—not merely wording or small date variations.

If fewer than two feasible scenarios exist, TaskMind must show the feasible option, if
any, and explain why alternatives are constrained. If none is feasible, it must identify
the unschedulable work and blocking constraints rather than conceal it or fabricate a
recovery. An infeasible draft may be shown for learning only when it is unmistakably
labeled, cannot be selected or applied, and lists its violations.

Generated explanations distinguish recorded facts, deterministic calculations,
assumptions, and AI interpretation. Users may regenerate against a refreshed baseline;
regeneration creates a new version and preserves prior versions and review activity.

## 8. Deterministic scenario constraints

Before comparison, after every material edit, and immediately before application,
authoritative services validate all proposed moves against:

1. **Working hours and leave.** Schedule only within the person's configured work
   calendar, time zone, holidays, and approved leave unless an explicitly permitted,
   separately confirmed exception exists.
2. **Allocation limits.** Include existing work and proposed changes across relevant
   projects; never exceed configured daily, weekly, role, or portfolio limits.
3. **Required skills.** Match verified required skills, proficiency or certification,
   and validity dates. Unknown skill data does not count as a match.
4. **Task dependencies.** Preserve graph validity, required ordering, lead/lag time,
   locked dates, and completion prerequisites; reject cycles.
5. **Milestone commitments.** Distinguish forecast from commitment and require the
   configured authority for date, scope, or success-criteria changes.
6. **Project access.** Verify that the proposing, consulting, approving, and affected
   users can see or modify only the records their roles require. Redact restricted
   cross-project details while reporting that a blocking constraint exists.
7. **Locked or externally managed work.** Do not edit locked records or work owned by an
   external system; use supported requests, links, or escalations and wait for confirmed
   synchronization.
8. **Regulatory or approval requirements.** Preserve mandatory gates, segregation of
   duties, certifications, evidence, retention, and approval ordering. Optimization may
   not bypass governance.

Validation results identify the violated rule, affected action, severity, authoritative
source and check time, and a safe correction when one exists. Blocking violations make a
scenario ineligible for approval. Warnings require acknowledgement or resolution under
policy and remain in the audit record.

## 9. Decision workflow

1. **Open risk.** An authorized manager opens a high-risk milestone and sees the trigger,
   baseline, data freshness, current forecast, scope, and known constraints.
2. **Generate alternatives.** TaskMind generates two to four meaningfully different
   recovery scenarios, or clearly explains why fewer feasible alternatives exist.
3. **Validate moves.** Deterministic services validate every proposed action and mark
   each scenario feasible, feasible with warnings, or infeasible.
4. **Compare impact.** The manager compares scenarios side by side using consistent rows
   for date/confidence, scope, people/teams, capacity, dependencies, cost/effort, new
   risks, reversibility, assumptions, rationale, and approvals. Baseline and deltas remain
   visible.
5. **Consult owners.** TaskMind requests input from affected owners when policy requires
   it. Responses, requested edits, objections, delegation, expiry, and non-response are
   visible and audited; consultation is not silently treated as approval.
6. **Select and edit.** The manager selects a feasible scenario and may edit supported
   actions. Each material edit creates a version, triggers revalidation, refreshes the
   forecast, and invalidates approvals or consultations affected by the change.
7. **Review final summary.** TaskMind presents an exact final change summary grouped by
   record and action, with before/after values, irreversible actions, warnings,
   notifications, approval requirements, and rollback coverage.
8. **Approve and apply.** Authorized approvers explicitly approve the exact scenario
   version. TaskMind applies it only after a fresh authorization, policy, constraint, and
   staleness check.
9. **Retain audit history.** The immutable baseline, proposals, edits, validations,
   consultations, approvals, application result, and any rollback remain inspectable.
10. **Monitor recovery.** TaskMind monitors forecast, confidence, execution signals, new
    risks, and milestone outcome against the baseline and approved scenario, then reports
    whether the forecast improved, held, or worsened.

Closing or navigating away from the workflow must not apply a proposal. Rejection records
the reason and leaves the plan unchanged.

## 10. Safety and integrity rules

- **Never silently reassign a person.** Ownership changes appear explicitly in the final
  summary and require confirmation and consultation or acceptance according to policy.
- **Never change an external commitment without confirmation.** TaskMind may propose a
  commitment change, but it remains unapplied until the designated authority confirms
  the exact date, scope, and version.
- **Never exceed configured capacity.** Overtime or exception capacity counts only when
  policy permits it and the required person and approver explicitly confirm it.
- **Identify unschedulable work.** Show the work, blocking constraint, and forecast effect;
  do not omit it from calculations or move it to an invisible state.
- **Apply atomically where required.** A scenario declares its atomic change groups.
  Preflight all operations and prevent partial application when partial state would break
  dependencies, approvals, ownership, or commitments. If an atomic group fails, none of
  its changes take effect and the failure is reported.
- **Detect stale proposals.** Bind every scenario to record versions and a baseline
  timestamp. Immediately before application, reject or require rebase and reapproval when
  relevant tasks, capacity, leave, dependencies, access, locks, policies, scope, forecast,
  or commitments changed.
- **Support rollback where reversible.** Capture compensating operations and their
  eligibility window. Rollback requires authorization and fresh validation, never claims
  to reverse irreversible external communications or completed work, and is itself
  audited.

Application must be idempotent: retrying the same approved version cannot duplicate
tasks, dependencies, approval checkpoints, escalations, notifications, or audit events.
Notifications are sent only after the relevant changes commit successfully.

## 11. Approval, consultation, and audit requirements

The final approver sees the same immutable scenario version that will be applied. Approval
records include actor, authority, decision, timestamp, scenario and baseline versions,
material warnings, required acknowledgements, and optional comment. A changed scenario
cannot reuse approval for an earlier version unless policy explicitly classifies the
change as nonmaterial.

Consultation policy may be triggered by reassignment, material allocation change, scope
change, team boundary, external commitment, regulated work, cost threshold, or
irreversible action. The system distinguishes **consulted**, **accepted**, **approved**,
and **notified**; none implies another.

The audit trail retains:

- the original plan snapshot and authoritative source versions;
- generation inputs, scenario versions, assumptions, and explanations;
- deterministic validation results and data freshness;
- manager edits and scenario selection or rejection;
- owner consultations, comments, objections, and responses;
- approval requests and decisions;
- application operation results and idempotency identity;
- notifications, external escalation status, and synchronization outcomes;
- rollbacks or compensating actions; and
- post-application forecast observations and milestone outcome.

Audit views must honor current access and retention policy without altering the integrity
of retained records.

## 12. Monitoring and exception handling

After application, monitoring begins from the application timestamp and compares the
approved scenario with both the original baseline and actual execution. The manager sees
forecast date and confidence movement, scope completion, dependency status, capacity
variance, new risks, assumption failures, and whether expected approvals or escalations
occurred.

TaskMind notifies the accountable manager when the forecast fails to improve within the
configured observation window, confidence materially declines, an assumption fails, an
external dependency misses its response date, or the plan becomes infeasible. It may
offer a new recovery cycle, but must not automatically modify the approved plan.

If application fails, the product identifies whether no changes were made, an atomic
group was rolled back, or non-atomic independent groups succeeded. Partial application is
allowed only when explicitly declared safe before approval; the final summary must show
that boundary, and failures remain actionable and auditable.

## 13. Success measures

Metrics must be segmented by workspace, milestone risk band, intervention type, and team
count where privacy policy permits. Report medians and distributions in addition to
aggregates, and define observation windows before the pilot.

| Measure                                             | Definition                                                                                                                                                                                                                                   |
| --------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| High-risk milestones with an approved recovery plan | Number of eligible high-risk milestones with a recovery scenario approved within the response window divided by eligible high-risk milestones opened or detected in that period. Report cases where no feasible scenario existed separately. |
| Time from risk detection to approved action         | Elapsed time from the recorded high-risk trigger to approval of an applicable scenario; report active user time and wall-clock time separately where measurable.                                                                             |
| Forecast improvement after application              | Change in forecast lateness and confidence from the frozen baseline to configured post-application checkpoints, adjusted for approved commitment changes and reported by intervention type.                                                  |
| On-time delivery rate after intervention            | Intervened milestones completed by the final explicitly approved committed date divided by intervened milestones reaching a terminal outcome; also report against the original commitment to avoid hiding date movement.                     |
| Generated scenarios rejected as infeasible          | Scenarios failing deterministic validation divided by all generated scenarios, with violations grouped by constraint and generation version. User rejection for preference is a separate measure.                                            |
| Manual planning steps avoided                       | Difference between a documented baseline workflow's comparable manual actions and the user actions required with TaskMind, using an agreed step taxonomy and excluding unattended computation.                                               |

Guardrail measures include unauthorized application attempts, stale-proposal rejection,
capacity violation prevention, atomic application failure, rollback success, unconfirmed
reassignment attempts, external commitment confirmation failures, consultation cycle
time, and assumption failure rate. Success metrics must not be used to rank individual
employees.

## 14. Acceptance criteria

The first release is acceptable when all of the following are demonstrated:

1. A manager can open a high-risk milestone and compare two to four materially different
   feasible scenarios when sufficient alternatives exist.
2. Every scenario contains all fields in section 6 and clearly represents unavailable
   information.
3. Every proposed move is deterministically validated against every applicable constraint
   in section 8 before comparison and again before application.
4. An infeasible scenario cannot be selected or applied, and unschedulable work remains
   visible with its blocking reasons.
5. A reassignment cannot occur silently or assign a person without verified skills,
   availability, allocation headroom, access, and required consultation or approval.
6. An external commitment cannot change without authorized confirmation of the exact
   scenario version.
7. A material edit triggers a new forecast and validation and invalidates affected prior
   consultation or approval.
8. The final summary shows exact before/after changes, irreversible actions, warnings,
   notifications, approvals, and rollback coverage before explicit approval.
9. A concurrent relevant change makes the proposal stale and prevents application until
   it is rebased, revalidated, and reapproved as required.
10. An atomic change group cannot leave partial business state after failure, and retrying
    an applied scenario does not duplicate effects.
11. Authorized users can inspect the original plan and complete consultation, approval,
    application, monitoring, and rollback history.
12. Post-application monitoring reports forecast and confidence movement against the
    original baseline and approved plan without autonomously changing either.

## 15. Architectural boundaries for future implementation

This PRD intentionally does not prescribe endpoints, schemas, events, or component
design. A future implementation must preserve TaskMind's service boundaries:

- **Core owns workflow and accepted business state.** It authorizes access, versions the
  baseline and proposal workflow, coordinates deterministic validation, consultations,
  approvals, atomic application, idempotency, audit, and the frontend facade.
- **Nova owns generation and explanation.** Prompts and model calls produce structured
  scenario candidates and explanations; Nova cannot apply task, ownership, scope,
  milestone, approval, or dependency changes.
- **Relay owns analytics and read context.** It supplies permission-scoped projections,
  risk signals, forecast observations, and recovery measurement; it does not become the
  source of accepted plan state.
- **The frontend calls only Core.** It does not call Nova or Relay directly.

Authoritative deterministic calculations must remain reproducible independently of
model output. Detailed service contracts belong in their owning build-kit references
when implementation is planned.

## 16. Open product decisions before implementation

- the high-risk threshold, manual entry reasons, and eligible milestone population;
- forecast method, confidence scale, calibration standard, and observation checkpoints;
- what constitutes a materially different scenario;
- default atomic boundaries and which independent groups may safely apply separately;
- consultation triggers, response deadlines, non-response behavior, and delegation;
- material-edit rules that invalidate consultation or approval;
- capacity granularity and policy for explicitly approved overtime or exceptions;
- verified skill sources, certification expiry behavior, and qualification governance;
- cost sources, currency conversion timestamp, uncertainty display, and visibility rules;
- commitment classifications and authority for internal, customer, contractual, or
  regulatory changes;
- rollback eligibility windows and irreversible action taxonomy;
- retention and redaction rules for planning snapshots, personal schedule data, and
  audit evidence; and
- pilot baselines, metric windows, step taxonomy, and target values for section 13.
