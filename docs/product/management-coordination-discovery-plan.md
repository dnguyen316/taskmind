# Management Coordination Discovery and Pilot Decision Plan

**Status:** Proposed; discovery evidence is not yet collected

**Owner:** Product

**Decision review:** After five qualified customer interviews and concept tests

**Scope:** Planning and validation only; this document does not authorize production changes

## 1. Purpose and decision standard

This plan tests whether TaskMind should initially serve management coordination in one
primary market segment. It separates facts about the current product from discovery
hypotheses. Values marked **hypothesis** are recruiting and interview anchors, not measured
customer results. They must be replaced by sourced observations before a pilot or release
decision.

The discovery phase exits only when all of the following are true:

1. The primary segment below remains selected or is replaced in a documented decision.
2. At least three workflows have quantified, customer-observed baselines covering every
   field in the baseline scorecard.
3. At least 60% of interviewed target users independently identify the same two problems
   among their top two management problems. With the minimum five interviews, at least
   three people must name both problems in their top two; do not combine prompted answers
   with unprompted answers.
4. At least one qualified pilot customer agrees in writing to its baseline, measurement
   method, data access, named owner, pilot duration, and success threshold.

Five interviews are the minimum evidence threshold, not a claim that validation has
already occurred. No cell in the evidence register may be marked complete without a date,
role, company qualification, notes link, and concept-test result.

## 2. Primary ideal customer profile

### Selected initial segment

The initial recommendation is **software and professional-services organizations with
50–1,000 employees**, narrowed for recruiting to organizations that coordinate
cross-functional, deadline-driven work across at least three teams. These organizations
should have enough coordination cost to measure while remaining able to run a focused
pilot without a multi-year transformation program.

This is one segment for the first discovery cycle, with two comparable operating models:

- software organizations delivering products, platforms, or internal technology; and
- professional-services organizations delivering client projects or retained services.

Results must be segmented by operating model. If their top problems, workflow baselines,
or required integrations diverge materially, choose the model with the stronger evidence
rather than averaging incompatible results.

### Firmographic and operational qualification

| Dimension | Required fit | Evidence to capture |
| --- | --- | --- |
| Organization size | 50–1,000 employees; pilot unit of 20–150 contributors and 3–15 managers | Total headcount, pilot-unit headcount, manager count |
| Workflow maturity | Repeatable monthly/quarterly planning and weekly reporting exist, but coordination still depends on spreadsheets, meetings, chat, or manual chasing | Current cadence, documented process, owners, exception paths |
| Work shape | Multiple concurrent projects, shared specialists, cross-team dependencies, and milestones visible to leadership | Active projects, shared roles, dependency examples, milestone count |
| Integration environment | A system of record such as Jira or GitHub plus workplace chat and a document/wiki tool; calendar and identity provider are common | Product names, deployment model, API/admin access, data residency |
| Compliance expectations | Role-based access, least privilege, auditability, retention/deletion controls, encryption, SSO expectation, data residency and AI-data-use clarity | Security questionnaire, required certifications, prohibited data, procurement gates |
| Buying trigger | Reporting burden, unreliable forecasts, missed dependencies, margin pressure, or a leadership mandate for responsible AI | Trigger, deadline, budget source, executive sponsor |

Disqualify teams that have no recurring cross-team workflow, cannot expose even
de-identified workflow data, want autonomous unreviewed business-state changes, or need a
first-release integration outside the minimum set before they can test any concept.

### Personas and jobs

| Persona | Typical title | Primary job and pain | Decision role | Concept-test emphasis |
| --- | --- | --- | --- | --- |
| Buyer | COO, VP Engineering, VP Professional Services, Chief of Staff | Improve delivery predictability and return management time without adding reporting headcount | Economic buyer; approves pilot and success criteria | Outcome value, risk, cost, deployment, executive reporting |
| Administrator | IT/SaaS admin, PMO operations lead, security admin | Connect systems, map permissions, govern AI use, maintain data freshness, and answer audits | Technical approver and operator | SSO/RBAC, audit, retention, integration health, policy controls |
| Manager | Engineering manager, delivery director, program/project manager | Plan work, balance capacity, surface blockers, review risk, and assemble status | Champion and daily reviewer | Review queue, provenance, edits, alerts, plan/status generation |
| Contributor | Engineer, consultant, designer, analyst | Capture actions and update work once without duplicative status requests | Data producer and proposal subject | Low-friction capture, notification load, accuracy, correction and opt-out |

Recruiting must include at least two managers, one buyer, one administrator, and one
contributor across the five-customer minimum. When one person holds two roles, record both
but count the interview once.

## 3. Current TaskMind baseline and reusable assets

The product baseline is a set of reusable capabilities, not proof of the management value
proposition:

- Core already owns tasks, projects, hierarchy, releases, scheduler state, comments,
  notifications, and Jira/GitHub/wiki integrations; Relay owns analytics projections and
  weekly-review/dashboard/project-health context; Nova owns prompts and AI audit
  (`../build-kit/00-overview.md`).
- The catalog defines human-reviewed AI capture, goal breakdown, scheduling proposals,
  weekly review, dashboard insights, project briefs, and spec breakdown. AI output remains
  a draft until accepted, and audit metadata includes model/provider, prompt version,
  latency, validation outcome, and user decision
  (`../build-kit/reference/ai-capabilities.md`).
- Backend history records capture accept/reject funnel events, typed weekly-review and
  project-brief capabilities, report availability/freshness metadata, notification
  delivery, and idempotent Jira/GitHub import (`../backend-feature-changelog.md`).
- Frontend history records review panels, scheduler proposal review, live team workload,
  reports/dashboard surfaces, project health cards, degraded-mode notices, and unavailable
  report states (`../frontend-feature-changelog.md`).

These references describe implemented or planned platform surfaces at different maturity
levels. Discovery facilitators must demo clickable concepts rather than imply every
catalog entry is production-ready. In particular, unavailable report values must never be
presented as zero, generated narrative must be distinguishable from facts, and acceptance
must remain explicit.

## 4. Workflow discovery protocol

### Interview method

Run a 60-minute session using a recent completed cycle, not opinions about an ideal future:

1. **Context (5 minutes):** role, team, projects, systems, compliance constraints.
2. **Artifact walkthrough (20 minutes):** ask the participant to reconstruct the last
   occurrence using redacted plans, reports, meeting notes, messages, or dashboards.
3. **Baseline count (15 minutes):** record the scorecard fields and calculation notes.
4. **Problem ranking (5 minutes):** independently rank problems before showing concepts.
5. **Clickable concept test (12 minutes):** scenario tasks with think-aloud observation.
6. **Pilot close (3 minutes):** confirm data access, stakeholder, and measurable threshold.

Ask “what happened last time?” and “show me where possible.” Do not ask whether the person
“likes AI.” Capture separate values by role and workflow. Obtain consent, redact customer
and personal data, and store notes under the approved retention policy.

### Recurring workflows and discovery prompts

| Workflow | Start and end boundary | Evidence and prompts | Candidate TaskMind concept |
| --- | --- | --- | --- |
| Quarterly and monthly planning | First collection of priorities through approved, assigned milestones | Who reconciles strategy, backlog, capacity, dependencies, and carryover? What changes after approval? | Evidence-linked plan proposal with capacity/dependency conflicts and explicit approval |
| Weekly status reporting | Request for updates through distributed and acknowledged report | Which systems and people supply updates? Which facts are stale? Who rewrites the narrative? | Draft weekly review with freshness, citations, missing-data flags, and accept/edit/reject |
| Meeting action capture | Meeting begins through actions accepted into the work system | Who takes notes, resolves owner/date ambiguity, enters tasks, and checks acceptance? | Notes-to-action drafts with owner/due-date confidence and batch review |
| Work assignment and capacity balancing | Work becomes assignable through owner commitment and a feasible schedule | How is skill, leave, utilization, WIP, and urgency reconciled? Who is overloaded? | Workload view plus explainable assignment/schedule proposals; no silent reassignment |
| Dependency and blocker follow-up | Dependency/blocker is recorded through resolution or accepted mitigation | Where are dependencies represented? How often are owners chased? What escalation is late? | Dependency queue, freshness timer, owner nudges, and proposed escalation |
| Delivery-risk review | Review preparation starts through risks have owners/actions | What makes a risk credible? Which signals are false or missing? How are mitigations tracked? | Project-health brief separating facts from inferred risks with reason and dismissal feedback |
| Executive reporting | Reporting request through leadership consumption/decision | Which portfolio metrics are reconciled? How many narrative revisions occur? What decisions follow? | Portfolio brief with metric availability, freshness, drill-down evidence, and export |

### Baseline measurement definitions

For each observed occurrence, capture:

| Field | Operational definition |
| --- | --- |
| Frequency | Occurrences per month; convert quarterly to 0.33/month and weekly to 4.33/month while retaining raw cadence |
| Participants | Unique people preparing, reviewing, approving, or manually supplying information; report median and range |
| Preparation time | Sum of active human minutes before the principal review/meeting, excluding passive waits; report person-hours per occurrence |
| Systems consulted | Distinct applications or material offline sources opened to complete the workflow; multiple pages in one product count once |
| Manual follow-ups | Human-authored reminders, direct messages, emails, or meeting chases caused by missing/late information per occurrence |
| Information delay | Elapsed business hours between intended completion and usable decision/report caused by missing or inaccurate information; record zero explicitly |
| Existing software cost | Allocated monthly license and operating cost for tools materially used by the pilot unit; show allocation method and currency |

Also capture rework time, decision/output, data owner, and confidence (`observed`,
`artifact-derived`, or `estimated`). Use medians across occurrences and customers; retain
ranges so a single large account does not dominate.

### Baseline scorecard: hypotheses to replace with observations

The table provides quantified assumptions for all seven workflows and therefore supports
sample-size and value-model planning. **None is a measured TaskMind result.** At least
three rows require customer-observed values before exit.

| Workflow | Frequency / month | Participants | Prep hours / occurrence | Systems | Manual follow-ups | Missing-info delay | Allocated software cost / pilot unit / month |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Quarterly/monthly planning | 1.33 | 10 | 18.0 | 5 | 8 | 2 business days | $1,500 |
| Weekly status reporting | 4.33 | 8 | 6.0 | 4 | 6 | 1 business day | $900 |
| Meeting action capture | 12 | 6 | 1.5 | 3 | 3 | 4 business hours | $500 |
| Assignment/capacity balancing | 4.33 | 7 | 4.0 | 4 | 5 | 1 business day | $1,000 |
| Dependency/blocker follow-up | 8 | 5 | 2.0 | 4 | 7 | 2 business days | $700 |
| Delivery-risk review | 4.33 | 7 | 5.0 | 5 | 5 | 1 business day | $1,100 |
| Executive reporting | 1 | 9 | 12.0 | 6 | 8 | 2 business days | $1,500 |

For each customer, calculate monthly coordination effort as:

`frequency per month × preparation person-hours per occurrence`.

Do not multiply preparation time by participants if the reported value is already summed
person-hours. Record participant hours individually when possible. Software costs across
workflow rows overlap and must not be summed without de-duplicating shared licenses.

## 5. Product metric tree

### North-star metric

**Management coordination hours returned per active organization (MCHRO)** is the monthly
sum of eligible baseline coordination minutes minus observed assisted-workflow minutes,
divided by 60, for organizations meeting the active definition.

An **active organization** has at least five active users spanning at least one manager and
three contributors, one fresh integration sync, and two completed eligible workflow runs
in the trailing 28 days. “Returned” hours count only matched workflows with a documented
baseline; extrapolation must be labeled separately. Report median, distribution, and
90-day retention, not only a total.

### Metric tree and definitions

| Branch | Metric | Definition |
| --- | --- | --- |
| Adoption | Eligible workflows | Distinct configured workflow instances that meet required data/freshness rules in 28 days |
| Adoption | AI-generated proposals | Valid proposals shown to an authorized reviewer; exclude retries, fallbacks not shown, and duplicates |
| Adoption | Review completion | Reviewed proposals with accept, edited-accept, or reject decision / proposals shown |
| Adoption | Acceptance rate | Accepted plus edited-accepted proposals / completed reviews; split unedited and edited |
| Quality | Edit distance | Normalized field-aware difference between proposal and accepted output; report text, date, owner, status, and structure separately |
| Quality | Rejection reason | Distribution over required controlled codes plus optional redacted comment: inaccurate, incomplete, stale, unsafe/policy, irrelevant, duplicate, or other |
| Quality | Rollback rate | Accepted AI-originated changes reversed or materially corrected within 7 days / accepted changes |
| Quality | False-risk rate | Risks dismissed as not real or unsupported / AI-raised risks reviewed; sample audits must test false negatives separately |
| Outcome | Forecast accuracy | Difference between forecast and actual milestone date, reported as median absolute error in days and percent within tolerance |
| Outcome | Blocker resolution time | Median elapsed business hours from blocker creation to resolved state; stratify severity |
| Outcome | Reporting time | Median active person-hours from report preparation start to approved distribution |
| Outcome | On-time milestone delivery | Milestones completed by committed date / due milestones, excluding documented scope-cancelled items |
| Guardrail | AI cost | Provider plus supporting AI infrastructure cost per active organization and per accepted proposal |
| Guardrail | Latency | Proposal time-to-first-useful-render and completion latency at p50/p95 by workflow |
| Guardrail | Policy violations | Confirmed access, data-use, retention, unsafe-action, or disclosure violations per 1,000 runs; severity always shown |
| Guardrail | Data freshness | Age of newest required source at generation and percent of runs meeting workflow freshness SLO |
| Guardrail | User complaints | Unique substantiated AI/workflow complaints per 100 active users, categorized by harm and workflow |

The causal reading is: eligible workflow coverage × proposal generation × completed review
× accepted time saved produces returned hours, while proposal quality sustains adoption and
outcomes. Guardrails can stop expansion even when MCHRO rises. Instrument proposal ID,
organization, workflow, source timestamps, review decision, changes, reversal, latency,
and cost without storing unnecessary prompt content or personal data.

## 6. Five-customer proposition validation

### Clickable concepts, not implemented features

Build a linked prototype in a design tool or a static isolated prototype with synthetic
data. It must not call production services, mutate real work, or be described as shipped.
Create three end-to-end concepts:

1. **Weekly coordination review:** source freshness → generated status → missing facts →
   citations → edit/accept/reject → distribution preview.
2. **Dependency and delivery-risk desk:** ranked blocker/risk queue → evidence → owner and
   age → dismiss reason or mitigation proposal → approval preview.
3. **Planning and capacity review:** goals/backlog → capacity/conflict view → proposed
   assignments and milestones → rationale → explicit approval preview.

Include failure and governance states: stale source, unavailable metric, low confidence,
permission denied, policy-blocked action, rollback, and audit history. Use neutral labels
such as “Concept” and “Simulation.” Do not seed the scenario with the hypothesized top
problems before the unprompted ranking.

### Task-based test and pass criteria

Each participant completes the same tasks: find what is stale; identify the highest-risk
commitment and its evidence; correct a wrong owner/date; reject an unsupported risk; find
who approved a proposed change; and explain whether they would pilot the workflow.

Record task completion without help, time on task, critical errors, confidence (1–5),
trust (1–5), perceived weekly minutes saved, and pilot commitment. A concept is promising
when at least four of five complete the core review without a critical error, median trust
is at least 4/5, and at least three would sponsor or actively use a measured pilot. These
concept criteria supplement rather than replace the 60% shared-problem exit criterion.

### Evidence register

| Customer | Segment qualification | Roles interviewed | Unprompted top two | Baselines completed | Concept result | Pilot commitment | Evidence link/status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| C01 | Pending | Pending | Pending | Pending | Pending | Pending | Not recruited |
| C02 | Pending | Pending | Pending | Pending | Pending | Pending | Not recruited |
| C03 | Pending | Pending | Pending | Pending | Pending | Pending | Not recruited |
| C04 | Pending | Pending | Pending | Pending | Pending | Pending | Not recruited |
| C05 | Pending | Pending | Pending | Pending | Pending | Pending | Not recruited |

Add interviews beyond five if the first five split by operating model or role. Never turn
“Pending” into a favorable result based on sales notes alone.

## 7. Decision record: provisional pilot recommendation

**Decision status:** Provisional; ratify, amend, or reject after the evidence register and
exit criteria are complete.

### Highest-value management workflow

Prioritize **weekly status reporting combined with dependency/blocker follow-up** as one
weekly coordination review. The hypothesis is that its high cadence, repeated manual
chasing, and direct link to delivery risk provide faster measurable value than quarterly
planning or executive reporting alone. Keep the two component baselines separate so the
pilot cannot double-count saved time. This choice is invalidated if fewer than 60% of
interviewees put status accuracy/effort and blocker follow-up in their unprompted top two.

### Minimum viable integration set

1. **One work system:** Jira Cloud **or** GitHub, selected per pilot, for work, ownership,
   status, dependencies/links, and milestones.
2. **One collaboration channel:** Slack for update requests and report delivery.
3. **Identity:** the pilot customer's SSO/identity provider plus TaskMind RBAC mapping.

Wiki/document context is read-only and optional only if the customer proves it is required
for weekly status accuracy. Calendar, email ingestion, CRM, HRIS, finance, and additional
work systems are excluded from the minimum set. TaskMind's existing Jira/GitHub,
notification, task/project, analytics, and AI-review foundations make this the shortest
credible integration hypothesis; actual connector readiness must be verified before a
pilot promise.

### Required enterprise controls

- SSO, organization isolation, role-based least privilege, and source-permission-aware
  retrieval.
- Human approval for generated reports and every business-state mutation; bulk or
  high-impact actions require explicit confirmation.
- Immutable, exportable audit trail for source freshness, proposal, prompt/model version,
  reviewer decision, edit, action, and rollback.
- Tenant-level AI enable/disable, approved capability/model policy, retention/deletion,
  provider data-use disclosure, prompt/response redaction, and no customer-data training
  without explicit agreement.
- Encryption in transit/at rest, secrets management, configurable data residency,
  incident handling, access review, and integration health/freshness monitoring.
- Idempotency, optimistic concurrency, rollback/correction paths, cost caps, rate limits,
  and policy-violation kill switch.

Certification commitments (for example SOC 2 or ISO 27001) must be established through
customer procurement discovery; do not claim certifications from this plan.

### Quantified pilot success threshold

Run a 6-week pilot after a 2-week baseline with 3–10 managers and 20–75 contributors. The
pilot succeeds only if all mandatory conditions hold:

- at least **20 management coordination hours returned per active organization per
  month**, calculated from matched weekly reporting and blocker-follow-up occurrences;
- at least **30% lower median reporting preparation time** versus the agreed baseline;
- at least **25% lower median blocker resolution time**, with severity mix reported;
- at least **70% review completion** and **60% acceptance** across at least 20 shown
  proposals, with edited acceptance reported separately;
- no severity-one policy violation, no cross-tenant or unauthorized disclosure, rollback
  rate at or below **5%**, and false-risk rate at or below **15%**;
- p95 proposal completion latency at or below **30 seconds**, required-source freshness
  SLO met on **95%** of runs, and AI variable cost at or below **$10 per active manager per
  month**; and
- the customer sponsor agrees the result justifies a paid expansion or a documented
  follow-on, based on the pre-agreed measurement sheet.

If volume is below 20 proposals, extend the pilot rather than declaring success on an
unstable rate. Freeze definitions before the pilot, report confidence and missing data,
and do not substitute satisfaction for outcome improvement.

### Explicit first-release exclusions

- Autonomous task assignment, status changes, schedule changes, escalations, or external
  messages without human approval.
- General-purpose Nova chat as the primary workflow, custom customer prompt builders, or
  open-ended agents with unrestricted tools.
- Full quarterly portfolio planning, financial forecasting, utilization billing, payroll,
  performance management, or employee surveillance/scoring.
- Meeting recording, transcription, and audio/video storage; the concept accepts pasted
  or approved text notes only.
- CRM, HRIS, ERP, email-ingestion, calendar-write, and multiple simultaneous work-system
  integrations.
- Custom executive report builders, mobile-native applications, and customer-specific ML
  model training.
- Unqualified guarantees of risk detection, delivery outcomes, regulatory compliance, or
  support for unavailable/stale metrics.

## 8. Decision meeting and artifacts

Before the decision review, Product publishes the completed evidence register, redacted
notes, baseline workbook, problem-ranking tally, concept usability results, integration
inventory, security-control gap assessment, and a pilot measurement agreement. The
decision owner records one outcome: **proceed to pilot**, **revise and re-test**, or **stop**.

Proceed only when every exit criterion passes and the provisional decision survives the
evidence. A decision note must state the selected operating model, observed top-two
problems, highest-value workflow, minimum integration set, enterprise controls, pilot
threshold, exclusions, dissenting evidence, and responsible owner/date. Until then, this
document is a testable plan rather than validation or authorization to change production.
