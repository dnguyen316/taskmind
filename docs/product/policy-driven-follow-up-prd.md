# Policy-Driven Follow-Up Product Specification

**Status:** Proposed pilot specification

**Owner:** Product

**Primary users:** Managers, accountable owners, program leads, and organization administrators

**Scope:** Detection, governed follow-up, acknowledgement, escalation, and resolution of
operational exceptions. This document defines product behavior; it does not authorize
production implementation or autonomous changes to underlying work.

## 1. Purpose

TaskMind should turn important, time-sensitive gaps in recorded work into a controlled
follow-up process. Today, managers often discover late work, aging blockers, missing
updates, or unanswered requests by inspecting several tools and manually sending repeated
messages. Policy-driven follow-up should detect a defined exception, identify the
accountable person, apply an organization-approved contact policy, and stop as soon as the
condition is addressed.

The product is not an unconstrained AI agent. Deterministic rules own detection,
deadlines, authorization, channel eligibility, suppression, and escalation. AI may make
the resulting communication easier to understand and act on, but may not decide whether
or when a person is contacted.

### 1.1 Goals

1. Resolve routine exceptions earlier and with fewer manager-authored follow-ups.
2. Give owners one clear, evidence-backed issue and an explicit next action.
3. Make every reminder, suppression, escalation, and state transition explainable and
   auditable.
4. Consolidate related signals so managers review cases rather than notification noise.
5. Respect access controls, working time, leave, communication preferences, and sensitive
   escalation approvals.

### 1.2 Non-goals

- Automatically changing owners, commitments, deadlines, status, capacity, or scope.
- Allowing AI to infer policy eligibility, permissions, or an escalation chain.
- Replacing the underlying task, project, dependency, approval, or reporting workflow.
- Sending arbitrary free-form campaigns or messages outside configured policies.
- Evaluating employee performance or generating performance scores.
- Resolving an exception merely because a reminder was sent or acknowledged.
- Supporting customer-defined executable code as a rule condition in the pilot.

## 2. Product principles

1. **Rules decide; AI explains.** A versioned deterministic policy produces every trigger,
   timer, contact, suppression, stop, and escalation decision.
2. **Evidence before interruption.** A recipient can see the exact facts, source age, and
   calculation that caused the exception, subject to their existing access.
3. **One condition, one active record.** Repeated observations refresh evidence on the
   active exception rather than create new alerts.
4. **Update at the source.** The preferred resolution path updates the underlying work;
   the exception then closes only after deterministic reevaluation.
5. **Least disruptive effective channel.** Low-severity items default to a digest, while
   more urgent contacts follow explicit channel and business-calendar policy.
6. **Human control for sensitive steps.** Policy can require approval before a configured
   escalation, recipient class, or channel is used.

## 3. Actors and permissions

| Actor                | Responsibilities                                                        | Product permissions                                                                                                            |
| -------------------- | ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| Accountable owner    | Address or route the underlying condition                               | View permitted evidence; acknowledge, update work, delegate where allowed, snooze, request help, dispute detection, or resolve |
| Manager / case owner | Manage related exceptions and intervene when automation is insufficient | Review ranked cases, take owner actions when authorized, bulk acknowledge, approve or initiate escalation                      |
| Policy administrator | Configure governed behavior                                             | Create and version policies, calendars, chains, channel limits, exclusions, and approval requirements                          |
| Escalation approver  | Review sensitive contact                                                | Approve or reject the exact pending escalation payload, recipients, channel, and reason                                        |
| Auditor              | Verify behavior without operating it                                    | Read policy versions, evidence, decisions, contacts, feedback, and state-transition history                                    |
| TaskMind automation  | Execute deterministic schedules                                         | Evaluate rules and suppression, enqueue eligible contacts, stop resolved workflows, and record immutable decisions             |
| Nova AI              | Assist with language and prioritization signals                         | Summarize, draft, recommend, and cluster within a deterministic envelope; never authorize or schedule actions                  |

All actions use existing organization and object authorization. Inclusion in an
escalation chain does not grant access to the affected work. When a recipient cannot view
supporting details, the channel payload contains only a policy-safe notice and a link to
an authorized in-product view, or the contact is blocked according to policy.

## 4. First supported exception types

Each detector runs from authoritative structured fields and a versioned policy. `T0` is
the time at which the trigger condition first became continuously true, not the time a
delayed detector happened to observe it.

| Exception type                                      | Required trigger inputs                                                                                                            | Default accountable owner                                     | Resolution condition                                                                                                                                         | Important safeguards                                                                                                     |
| --------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------ |
| Overdue high-priority task                          | Open task; priority in configured high-priority set; due instant before evaluation instant; grace elapsed                          | Task owner                                                    | Task is completed/cancelled, due date moves into an allowed future window, priority exits configured set, or task is excluded                                | A date change must be authorized and material; comments alone do not suppress indefinitely                               |
| Blocker exceeding an age threshold                  | Active blocker; blocker age in business time exceeds configured threshold                                                          | Blocked work owner, or blocker owner when explicitly recorded | Blocker resolved/accepted as mitigated, affected work cancelled, or policy no longer applies                                                                 | Paused time counts only when the blocker state explicitly supports pause; severity may reflect committed work impact     |
| Milestone forecast outside tolerance                | Current forecast differs from committed milestone date by more than configured absolute or percentage tolerance                    | Milestone owner                                               | Forecast returns within tolerance, commitment is validly rebaselined, milestone completes/cancels, or exception is accepted under an approved policy outcome | Missing forecast is not treated as zero and may be a separate data-quality gap; record forecast model/version            |
| Pending approval exceeding its service-level target | Approval request remains pending beyond its configured business-time SLO                                                           | Current approver                                              | Approved, rejected, cancelled, or validly reassigned                                                                                                         | Do not disclose protected approval content; SLA clock follows the assigned approver's calendar and pauses only by policy |
| Missing weekly status                               | Required status period closes plus grace with no qualifying status from the required scope                                         | Configured status owner                                       | A complete qualifying status is submitted for the period, requirement is waived, or the scope becomes ineligible                                             | Drafts and unrelated comments do not qualify; leave/delegation rules must be evaluated before contact                    |
| Unacknowledged dependency request                   | Active dependency request has no qualifying acknowledgement after target time                                                      | Requested dependency owner                                    | Acknowledged, accepted, declined with reason, delegated, cancelled, or dependency resolved                                                                   | Delivery receipt is not acknowledgement; requester activity must not reset the recipient timer                           |
| Work item without an owner                          | In-scope actionable item has no active accountable owner after creation/import grace                                               | Project or team triage owner                                  | Eligible owner assigned, item becomes non-actionable, or item is cancelled/excluded                                                                          | Do not infer or assign an owner with AI; distinguish invalid/deactivated owners from transient sync gaps                 |
| Capacity overload affecting a committed milestone   | Approved capacity model projects person/team load above configured limit and identifies impact to at least one committed milestone | Milestone owner, with overloaded team manager as participant  | Load returns within limit, commitment or allocation is validly changed, mitigation is accepted, or milestone completes/cancels                               | Show model inputs, leave, allocations, horizon, and confidence; never silently reassign work or change commitments       |

### 4.1 Detection contract

Every detector returns: policy and detector version, evaluation instant, `T0`, matched
facts, failed and satisfied predicates, source identifiers and freshness, calculated
thresholds in the applicable business calendar, and a stable deduplication key. A detector
must return `INDETERMINATE` rather than trigger when a required input is absent, stale, or
unauthorized. Indeterminate results appear in operational health and do not contact users.

## 5. Exception record

An exception is a durable workflow record, not a notification. It contains the following
logical fields.

| Field group         | Required contents                                                                                                                                                         |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Identity            | Stable exception ID; organization ID; exception type; detector version; policy ID and immutable policy version; deduplication key                                         |
| Classification      | Severity (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`); confidence (`HIGH`, `MEDIUM`, `LOW`) with reason; created and last-evaluated times                                        |
| Affected work       | Exactly one primary objective, project, milestone, or task reference plus permitted related references; source system and canonical link                                  |
| Accountability      | Accountable owner identity and role; manager/case owner when applicable; delegation history; assignment basis                                                             |
| Timing              | `T0`, detected time, grace expiry, target resolution time, next evaluation, and next eligible automation time, all with calendar/time-zone references                     |
| Supporting evidence | Structured facts, predicate result, source timestamps, freshness, threshold calculation, evidence links, and immutable evidence snapshots/hashes for sent contacts        |
| Business impact     | Affected commitment, delay/cost/customer/capacity indicator, impacted scope, and fact-versus-AI-summary labels; unknown impact is explicit                                |
| Automation          | Current step, step status, attempt count, next scheduled step, pending approval, last contact, last meaningful update, and suppression reason/expiry                      |
| User state          | Acknowledgement state, actor/time/note/promised-action time; resolution state, outcome/reason/actor/time; dispute and help-request state                                  |
| Escalation          | Ordered append-only history of evaluated, approved, rejected, sent, skipped, failed, and cancelled escalation steps with recipients, channel, reason, and policy decision |
| Audit               | Creation and transition actors, idempotency keys, policy evaluations, AI generation references, message render hashes, delivery outcomes, and retention markers           |

Evidence shown in a message is a least-privilege projection, not the complete record. AI
text is stored separately from source facts with model, prompt version, input evidence
IDs, generation time, and validation outcome.

### 5.1 Severity and confidence

Severity is determined by a policy table over exception type, lateness/age band,
commitment impact, and configured business impact. Confidence reflects data completeness,
freshness, and detector certainty; AI does not set either value. A policy version can
raise or lower severity on reevaluation, and each change is audited. Critical severity
does not bypass authorization, channel limits, quiet hours, or required approval unless
the policy explicitly defines a compliant emergency path.

### 5.2 Lifecycle

Primary exception states are:

`OPEN -> ACKNOWLEDGED -> RESOLVED`

Supporting states are `SNOOZED`, `HELP_REQUESTED`, `DISPUTED`, and `AWAITING_ESCALATION_APPROVAL`.
They coexist with an unresolved primary state. `CLOSED_INCORRECT`, `CANCELLED_BY_POLICY`,
and `EXPIRED` are terminal outcomes distinct from `RESOLVED`.

- Detection opens one record and schedules the first eligible step after grace.
- Acknowledgement stops only the contacts specified by policy; it does not assert that the
  underlying condition is fixed.
- A user's “resolve” action requests reevaluation. The record reaches `RESOLVED` only if
  the source condition is false or an explicitly allowed resolution outcome is recorded.
- A terminal record may reopen as a new occurrence only after the condition became false
  and later became true. The new record links to the prior occurrence.

## 6. Rule model

Policies are immutable once activated. Editing produces a new draft version with a
preview of affected scopes and simulated outcomes before activation.

| Rule element                  | Definition                                                                                                                                                                |
| ----------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Scope                         | Organization, team, project, or work type selectors; explicit inclusions/exclusions; deterministic precedence from most specific to organization default; effective dates |
| Trigger conditions            | Exception type, required structured predicates, thresholds, eligible statuses/priorities/commitments, freshness requirements, and detector version                        |
| Grace period                  | Business or elapsed duration between `T0` and first exception/contact; separate creation grace may apply to imports or owner assignment                                   |
| Business-hours calendar       | Time zone, working intervals, weekends, holidays, shutdowns, and leave behavior; defined clock owner for multi-party workflows                                            |
| Reminder sequence             | Ordered steps with offset, recipient role, template, channel priority, retry policy, acknowledgement behavior, and severity constraint                                    |
| Escalation chain              | Ordered, directory-resolved roles or named groups; fallback when a role is vacant; maximum depth; no cyclic recipient path                                                |
| Allowed channels              | In-app, email, and Slack in the pilot, intersected with organization policy, recipient preference, data classification, integration health, and authorization             |
| Stop conditions               | Underlying condition false, allowed terminal outcome, work cancellation, scope/policy ineligibility, contact cap, or administrator cancellation                           |
| Suppression conditions        | Meaningful source update, active snooze, leave/delegation, related open case, recent equivalent contact, digest deferral, quiet hours, or approved exclusion              |
| Maximum contacts              | Total per exception plus recipient/channel rolling-window caps; retries that reached a provider but lack acknowledgement count as contacts                                |
| Sensitive escalation approval | Step-level requirement, eligible approver role, expiry, exact payload snapshot, and reject/fallback behavior                                                              |

### 6.1 Policy precedence and conflicts

The most specific active policy wins for a single element: work item, project, team, then
organization. An explicit exclusion wins over inclusion at the same or broader scope.
TaskMind never merges two reminder sequences or escalation chains. A policy preview shows
which version wins and why. If policy resolution is ambiguous, automation fails closed,
records a configuration fault, and sends no contact.

### 6.2 Meaningful updates

A meaningful update must change a field named by that exception type's suppression
contract and plausibly advance the condition. Examples include a blocker state or expected
unblock date, approval decision/delegation, dependency acknowledgement, owner assignment,
qualifying weekly status, milestone forecast/mitigation, or task status/due-date change.
Comments, reactions, views, unrelated task changes, sync heartbeats, and automated
timestamp churn are never meaningful by default.

Suppression after a meaningful update has a bounded duration and never resolves the
record unless reevaluation proves the stop condition. The decision records the changed
fields, source event, suppression expiry, and rule version. This contract directly guards
against suppressing an unresolved exception because another task changed.

## 7. Automation execution

For each due step, Core performs this order atomically and idempotently:

1. Reload the authoritative work state and reevaluate the detector.
2. Stop and resolve/cancel if a deterministic stop condition now holds.
3. Resolve the effective policy version and verify scope, recipient authorization, leave,
   working hours, suppression, contact caps, integration health, and channel eligibility.
4. If approval is required, freeze the intended recipients, evidence projection, channel,
   and rendered content for approval; do not send while approval is pending.
5. Optionally request an AI summary/draft inside the approved evidence and instruction
   envelope, validate its output, and use a deterministic fallback template on failure.
6. Recheck time-sensitive eligibility immediately before enqueueing delivery.
7. Record the decision and idempotency key, enqueue one contact, then record provider and
   delivery outcomes without treating delivery as acknowledgement.
8. Calculate the next eligible step from the policy and applicable business calendar.

Late or duplicated jobs must not duplicate contacts. A policy change affects future steps
only; already approved or sent steps retain the policy and render versions used.

## 8. User actions

| Action                       | Required behavior and guardrails                                                                                                                                                                                                 |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acknowledge                  | Records actor, time, optional note, and promised-action time; suppresses only policy-defined reminders; does not resolve the exception                                                                                           |
| Resolve                      | Requires an outcome and optional note; prompts source update when needed; deterministic reevaluation confirms closure or explains remaining predicates                                                                           |
| Update underlying work       | Opens an authorized, context-specific edit flow; normal validation, optimistic locking, and audit rules apply; no exception-only shadow fields                                                                                   |
| Delegate                     | Selects an authorized active delegate, reason, and optional expiry; records acceptance when policy requires it; updates future routing but not historical accountability                                                         |
| Snooze with a reason         | Requires a controlled reason, optional note, and bounded wake time; policy defines allowed duration/count; critical or approval-bound steps may disallow it                                                                      |
| Request help                 | Selects or policy-resolves an authorized helper, records the request, and prevents duplicate escalation while the help window remains active                                                                                     |
| Mark as incorrectly detected | Requires a controlled reason and optional evidence; pauses contacts, opens a review, and closes as `CLOSED_INCORRECT` only after authorized confirmation; feedback feeds detector-quality metrics, not model training by default |
| Escalate immediately         | Shows exact next recipient/channel/reason and approval requirement; can advance only to a policy-eligible step and cannot bypass authorization, caps, exclusions, or required approval                                           |

Every action is available from the in-app exception detail when authorized. Channel
messages use signed, short-lived deep links and expose only policy-approved quick actions;
sensitive edits and resolution require the in-app flow.

Bulk acknowledgement is allowed when the actor can acknowledge every selected record and
the UI shows the consequence. Bulk resolve is not supported in the pilot. Other bulk
mutations, including snooze, delegate, dispute, and immediate escalation, are disabled
unless separately safety-reviewed.

## 9. AI responsibilities and constraints

Nova may:

- summarize the issue, supporting facts, and stated business impact;
- draft a context-aware reminder in the configured tone and language;
- recommend a next action chosen from the deterministic set of currently allowed actions;
- identify likely related exceptions for case-grouping review; and
- explain why a ranked item may deserve attention using permitted evidence.

Nova must never determine or alter:

- access or evidence visibility;
- due dates, service-level targets, grace periods, or target resolution times;
- whether a trigger is true;
- severity, confidence, escalation eligibility, recipients, channels, or schedule;
- whether an update is meaningful;
- stop, suppression, acknowledgement, or resolution state; or
- whether sensitive escalation approval is required or satisfied.

AI outputs are proposals. Core supplies a structured, permission-filtered evidence bundle
and enumerated allowed actions; validates output against a schema; labels generated text;
and falls back to a deterministic template when generation is unavailable, unsafe, or
unsupported. Prompts must prohibit invented facts, blame, employee evaluation, and new
commitments. Generated content cannot add recipients or evidence.

## 10. Manager experience

### 10.1 Exception inbox

The manager inbox has two primary lanes:

1. **Needs human judgment:** disputed detections, help requests, sensitive escalation
   approvals, missing/low-confidence evidence, exhausted automation, and cases where the
   policy offers multiple consequential paths.
2. **Automation in progress:** acknowledged, snoozed, recently updated, awaiting the next
   reminder, or otherwise being handled without manager action.

Every row shows type, severity, affected work, owner, age in business time, target
resolution, impact, confidence with reason, acknowledgement state, current automation
step, suppression if any, and the next scheduled step. “No further automatic contact” is
shown explicitly.

### 10.2 Ranking

Items are ranked by a deterministic score derived from policy-defined impact, urgency,
and confidence bands. The UI exposes each component and supports stable sorting and
filtering. AI may generate a narrative explanation but cannot alter the score. Critical
items and approval deadlines remain visibly pinned according to policy.

### 10.3 Management cases

TaskMind proposes grouping exceptions when they share a root work item, dependency,
milestone, owner, or evidence-backed causal relationship. Exact-key groups can be created
deterministically; AI-proposed semantic groups require manager confirmation. A case has a
primary impact, case owner, related exception list, roll-up timeline, and coordinated
next step. Each exception keeps its own state, timer, evidence, policy, and audit history.
Resolving or suppressing a case never silently resolves its members.

### 10.4 Daily summary

Low-severity exceptions are collected into one recipient-specific daily summary during
the recipient's configured business window. The summary deduplicates case members,
separates new/due/updated items, and offers authorized acknowledgement links. An item
already contacted individually is not repeated in the same digest window unless its
severity increased. High/critical behavior follows policy and need not wait for a digest.

## 11. Anti-spam and safety controls

1. **Deduplicate repeated events.** The stable key comprises organization, type, primary
   affected object, policy-relevant dimension, and occurrence epoch. Event replay updates
   evaluation history but does not create or contact again.
2. **Suppress after meaningful updates.** Only the type-specific changed-field contract
   qualifies, suppression is time-bounded, and every due step still reevaluates the
   underlying condition.
3. **Respect working hours and leave.** Delivery is shifted to the next allowed window or
   routed to an accepted delegate according to policy. Leave never causes an unauthorized
   manager disclosure.
4. **Stop on resolution.** Source events and scheduled safety reevaluation cancel queued
   unsent steps once the condition is false. Delivery workers recheck cancellation before
   send where supported.
5. **Rate-limit recipient and channel.** Enforce organization-configured rolling caps by
   recipient/channel and a total exception cap. Deferred items appear in a digest or
   manager queue; they are not silently discarded.
6. **Honor organization exclusions.** Administrators can exclude scopes, work types,
   recipients, channels, classifications, or exception types with reason and effective
   dates. Exclusions are audited and visible in policy preview.
7. **Prevent echo and loops.** Bot-authored updates and delivery callbacks cannot count as
   owner acknowledgement or meaningful work updates. Escalation chains reject cycles.
8. **Fail closed.** Stale required inputs, unresolved policy conflicts, missing recipient
   authorization, and unavailable sensitive approvals result in no contact and a visible
   operational fault.

## 12. Notifications and channel behavior

Pilot channels are in-app, email, and Slack. Each contact must include a concise issue,
affected work, target time, evidence freshness, allowed next actions, and authorized deep
link. It must not include information the recipient cannot view in TaskMind.

Channel selection is the intersection of policy allowance, organization configuration,
data classification, recipient authorization and preference, working window, health, and
rate limits. A failed Slack delivery may fall back to email only if the policy explicitly
orders that fallback and all checks pass again. Provider retry, fallback, and human
reminder are distinct attempts in history.

## 13. Audit, observability, and administration

Administrators need:

- policy draft, validation, simulation, activation, rollback-to-new-version, and effective
  scope preview;
- business-calendar, leave-source, escalation-directory, and channel-health diagnostics;
- a dry-run mode that records would-trigger, would-suppress, and would-contact decisions
  without contacting anyone;
- per-exception decision traces showing predicates, calendar math, policy precedence,
  recipient resolution, caps, and stop/suppression results;
- contact-volume forecasts and actual volumes by type, severity, recipient, and channel;
- audit export with controlled access and retention; and
- alerts for detector lag, stale sources, delivery backlog, approval expiry, policy
  ambiguity, or an abnormal trigger/incorrect-feedback rate.

Sensitive message bodies follow the organization's retention and redaction policy.
Metric events use pseudonymous identifiers where full content is unnecessary.

## 14. Pilot design and targets

### 14.1 Eligibility and measurement

The pilot begins with organizations that configure at least two exception types, an
approved policy/calendar, valid owners, and one enabled channel. Run dry mode for at least
one representative business week, review projected contacts and false triggers, then
activate a bounded team/project cohort. Baselines use the four weeks before activation
when comparable data exists; the evaluation window is at least eight active weeks.

An **eligible exception** is a detector-confirmed occurrence with fresh required inputs,
an accountable owner, an active policy, and at least one permitted action. Dry-run,
test/synthetic, duplicate, indeterminate, excluded, and pre-existing already-resolved
occurrences are excluded from outcome denominators.

### 14.2 Success metrics

| Target                                                         | Definition                                                                                                                                                                                                                       |
| -------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| At least 40% resolved without manager intervention             | Eligible exceptions that reach a source-confirmed resolution before any manager-authored contact, manager action, or manager-approved sensitive escalation / all eligible exceptions reaching a terminal outcome in the window   |
| At least 30% reduction in manual follow-up messages            | `(baseline normalized manager-authored follow-ups - pilot normalized manager-authored follow-ups) / baseline normalized manager-authored follow-ups`; normalize per 100 eligible exceptions and compare the same cohort/channels |
| Less than 5% incorrect-trigger feedback                        | Unique eligible exception occurrences confirmed or still pending review as incorrectly detected / unique eligible exceptions shown to a user; report confirmed and pending separately                                            |
| No unresolved exception suppressed by an unrelated task update | Count of audited cases where an unrelated object/field event caused contact suppression while the trigger remained true; target exactly zero, validated by event-contract tests and pilot sampling                               |
| Median blocker acknowledgement time improves at least 25%      | Compare median business time from blocker `T0` to first qualifying acknowledgement for matched severity/cohort against baseline: `(baseline - pilot) / baseline >= 25%`                                                          |

Report all targets by exception type, severity, organization, and automated versus
manager-handled path. Also monitor contacts per recipient/week, opt-outs/exclusions,
snooze and dispute rates, escalation volume, after-hours attempts, delivery failures,
time-to-resolution, and manager intervention rate. Averages must not conceal a harmful
recipient-level tail.

### 14.3 Pilot guardrails and stop criteria

Pause the affected policy or cohort when any of these occurs:

- unauthorized disclosure or contact;
- after-hours or leave contact outside an explicitly approved emergency policy;
- unresolved exception suppressed by an unrelated update;
- duplicate or cap-violating contacts indicating an idempotency failure;
- sustained incorrect-trigger feedback at or above 10% for an exception type after a
  minimum reviewable sample; or
- inability to reconstruct a contact's policy, evidence, recipient, and decision trace.

Restart requires root-cause review, corrected tests/configuration, and designated product
and security/operations approval appropriate to the incident.

## 15. Acceptance criteria

### Detection and records

- All eight exception types produce stable, deduplicated records from deterministic,
  versioned predicates and return indeterminate on missing required evidence.
- Every record contains the defined identity, scope, owner, timing, evidence, impact,
  automation, state, escalation, and audit fields.
- Source-confirmed resolution stops queued future contacts, and unrelated updates never
  resolve or suppress an exception.

### Policy and automation

- Administrators can configure and simulate every rule-model element before activating an
  immutable policy version.
- Calendar calculations, policy precedence, stop/suppression, channel eligibility,
  contacts caps, approval, and idempotency have deterministic automated tests.
- Every contact can be reconstructed from its policy version, source-evidence snapshot,
  AI/fallback render, authorization decision, and delivery attempt.

### Owner and manager experience

- Authorized users can perform all eight actions with the stated constraints and can see
  why an action is unavailable.
- Managers can distinguish needs-judgment from automation-in-progress, inspect ranking
  components, see the next step, confirm case groups, and bulk acknowledge without an
  unsafe bulk-resolution path.
- Low-severity eligible contacts consolidate into a daily summary and do not also produce
  redundant individual messages in the same window.

### AI and safety

- AI can summarize, draft, recommend an allowed action, and propose groups, with factual
  provenance and deterministic fallback.
- Tests demonstrate that AI output cannot change access, dates, trigger state, severity,
  eligibility, schedule, recipients, channels, suppression, stop, or resolution.
- Rate limits, organization exclusions, working hours, leave, sensitive approvals, and
  least-privilege channel rendering are enforced immediately before delivery.

## 16. Open product decisions before implementation

1. Which severity matrix and default thresholds should ship as templates rather than
   organization defaults?
2. Which source event and field changes qualify as meaningful for each detector, and what
   is each bounded suppression window?
3. Does “missing weekly status” apply per person, team, project, or configurable reporting
   obligation in the first pilot?
4. Which approval content classes require payload freezing and dual control?
5. What minimum data quality and forecast-confidence thresholds make capacity-overload
   and milestone-forecast detectors eligible?
6. Which leave system is authoritative, and what occurs when leave data is unavailable?
7. What sample sizes and confidence intervals will accompany each pilot target so a small
   cohort is not overinterpreted?

These decisions must be resolved in versioned policy templates and detector contracts;
they must not be delegated to prompt wording or model behavior.
