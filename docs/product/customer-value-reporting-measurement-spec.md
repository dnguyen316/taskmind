# Customer-Visible Value Reporting Measurement Specification

**Status:** Proposed

**Owner:** Product, with Analytics and Finance review

**Scope:** Customer-visible value dashboards, exports, and pilot commercialization

## 1. Purpose and reporting principles

This specification defines how TaskMind reports customer value without presenting modeled
benefits as observed facts. It governs metric definitions, baseline collection, event
attribution, financial estimates, dashboard views, and pilot exit decisions. It does not
authorize autonomous changes to customer work or replace a customer finance review.

Every displayed value must be reproducible from a versioned methodology and must:

1. use a customer-approved, configurable baseline established during onboarding;
2. use observed workflow and outcome events wherever possible;
3. identify the result as **measured**, **estimated**, or **unavailable**;
4. avoid deriving all time savings from AI-generated estimates;
5. attribute overlapping benefits once through a shared workflow-run ledger; and
6. show confidence and methodology notes beside every financial estimate.

The reporting period uses the customer's configured timezone and calendar. Historical
results retain the baseline, rate card, currency, attribution rules, and methodology
version used when the period closed. A later configuration change applies prospectively
unless an authorized administrator explicitly requests and labels a restatement.

## 2. Measurement model

### 2.1 Units of analysis

The canonical unit is a **workflow run**: one bounded customer process with a stable
`workflowRunId`, organization, team, capability, start and completion time, and eligible
participants. Product events, human review decisions, outcome events, and AI costs attach
to that identifier. Examples include one weekly report, one meeting action review, one
risk review, or one recovery-plan proposal.

Each workflow run records:

| Field | Purpose |
| --- | --- |
| Organization, team, capability, and workflow type | Access control and dashboard grouping |
| Baseline version and measurement window | Reproducibility |
| Start, proposal, review, acceptance, completion, and outcome timestamps | Effort and cycle-time calculations |
| Actor role and pseudonymous actor identifier | Participant counts without unnecessary personal data |
| Source event identifiers | Evidence and deduplication |
| Proposal identifier and decision | Accepted-versus-rejected funnel |
| Outcome identifier and state | Delivery, exception, forecast, or recovery result |
| AI run identifier and allocated cost | Outcome-level cost reporting |
| Evidence class and confidence | Disclosure |

Raw evidence must follow the customer's retention policy. Customer-facing drill-downs
show source type, timestamp, and calculation notes but must not expose prompt content,
private message bodies, or data a viewer is not authorized to access.

### 2.2 Customer baselines

During onboarding, an administrator and a customer business owner configure a baseline
for each eligible workflow. The baseline contains:

- workflow boundary, frequency, roles, and teams;
- active human minutes per occurrence, split by preparation, administration, reporting,
  follow-up, exception handling, and management intervention where applicable;
- follow-up message count, exception count, forecast tolerance, delivery-breach rule,
  and other outcome-specific inputs;
- evidence source and sampling window;
- loaded hourly rate by role, currency, and whether the rate includes overhead;
- exclusions, known overlaps, confidence, approver, approval date, and effective dates.

Prefer time studies, system timestamps, calendar artifacts, and representative work
samples. Structured customer estimates are acceptable when observation is impractical,
but an LLM may only help summarize supplied evidence; its generated estimate cannot be
the sole baseline. Sample at least three representative occurrences when cadence permits,
show the median and range, and flag baselines older than the customer's configured review
interval (default: 180 days) as needing review.

### 2.3 Evidence classes and display states

| State | Minimum evidence | Display treatment |
| --- | --- | --- |
| **Measured** | Customer baseline plus matched product/workflow events and, for outcome metrics, an observed outcome event | Solid value, evidence coverage, source note, and confidence |
| **Estimated** | Customer baseline exists, but one or more inputs use an approved sample, allocation, or extrapolation | Value prefixed by “Estimated,” with modeled inputs and range |
| **Unavailable** | Baseline, required event coverage, permissions, or denominator is missing or invalid | “Unavailable” with the missing prerequisite; never zero |

Confidence is reported as **high**, **medium**, or **low**. High requires an approved
baseline and at least 90% required-event coverage; medium requires an approved baseline
and 60–89% coverage or one approved modeled input; low covers less than 60% coverage or
multiple modeled inputs. A metric with no credible baseline or denominator is unavailable,
not low-confidence. Dashboards must display coverage percentage and the methodology
version, and allow the customer to inspect the confidence rationale.

### 2.4 Attribution and double-count prevention

A workflow run may support several diagnostic metrics but may contribute saved minutes to
only one **primary time-value category**. The default precedence is:

1. planning time avoided;
2. meeting administration reduced;
3. reporting time reduced;
4. follow-up messages avoided; and
5. exceptions resolved without manager intervention.

Customers may configure a different mapping before a reporting period opens. Secondary
outcomes such as early risk detection, recovery-plan adoption, and forecast accuracy are
reported alongside the primary time value but do not add time or money unless a separate,
non-overlapping baseline activity is documented.

The attribution ledger enforces uniqueness on organization, workflow run, baseline
activity, and value category. If one event closes several steps, allocate observed minutes
across mutually exclusive activity buckets or assign the full saving to the primary bucket;
never copy the full saving into each metric. Aggregations deduplicate proposal retries,
duplicate events, reopened outcomes, and the same accepted outcome referenced by multiple
capabilities. Dashboard totals are computed from the ledger, never by summing independently
rounded cards.

### 2.5 Time and financial calculations

For a matched workflow run:

```text
eligible minutes returned = max(0, baseline active minutes - observed active minutes)
hours returned = sum(deduplicated eligible minutes returned) / 60
financial estimate = sum(hours returned by role × approved loaded hourly rate by role)
net financial estimate = financial estimate - AI operating cost
cost per accepted outcome = allocated AI operating cost / accepted outcomes
effort reduction % = eligible minutes returned / baseline active minutes × 100
```

Observed active time should come from workflow timestamps adjusted for passive waits, or
from explicit timer/activity events. If only completion boundaries exist, use a
customer-approved sampling factor and label the result estimated. Show gross value, AI
operating cost, and net estimate separately; do not imply realized cash savings, headcount
reduction, or revenue unless the customer supplies and approves that methodology.

Financial cards show currency, rate-card effective date, evidence state, confidence,
coverage, material exclusions, and a link or tooltip explaining the formula. Estimated
values show a range based on the approved baseline range rather than false precision.

## 3. Value categories

| Category | Definition and method | Required evidence |
| --- | --- | --- |
| Planning time avoided | Baseline active minutes for gathering inputs, reconciling dependencies/capacity, and drafting a plan minus observed assisted-workflow minutes | Approved planning baseline; plan workflow events; accepted/finalized plan |
| Meeting administration reduced | Baseline minutes for agenda/notes/action extraction and entry minus observed administration minutes; meeting duration itself is excluded unless explicitly in baseline | Meeting boundary; artifact generation/review events; accepted actions |
| Reporting time reduced | Baseline active preparation, reconciliation, and revision minutes minus observed time to approved distribution | Report workflow timestamps; approval/distribution event |
| Follow-up messages avoided | Expected baseline follow-ups for comparable runs minus observed human-authored reminders; optionally convert to time using a customer-approved minutes-per-message factor | Baseline count; messaging/reminder events or sampled audit; no AI-only estimate |
| Exceptions resolved without manager intervention | Eligible exception closes without a manager action between detection and resolution, divided by eligible exceptions; time value is separate only with a manager-intervention baseline | Exception lifecycle, actor roles, resolution and escalation events |
| Risks detected before a delivery breach | Confirmed delivery risks first surfaced before the breach or before the configured lead-time threshold; show count, rate, and median lead time | Risk detection/evidence, review decision, committed date, actual outcome |
| Recovery plans adopted | Recovery proposals accepted or edited-and-accepted and put into effect, divided by reviewed recovery proposals; adoption is not effectiveness | Proposal/review decision and evidence of plan activation |
| Forecast accuracy improved | Change from agreed pre-pilot/reference error to current forecast error, using median absolute error in days and percent within customer tolerance | Versioned forecasts made before outcomes; actual milestone dates; comparable cohort |
| AI operating cost | Provider token/inference charges plus directly allocable AI infrastructure cost; show total, per workflow, per accepted outcome, and per delivered outcome | AI run usage/cost, allocation version, workflow/proposal/outcome link |

Risk, recovery, and forecast improvements require matured outcomes. Open milestones remain
in the eligible cohort but not the resolved denominator; the view must disclose cohort
maturity and must not treat pending work as successful.

## 4. Audiences and access

| Audience | Primary questions | Default presentation |
| --- | --- | --- |
| Executive buyer | How many hours were returned, did delivery improve, and what is the credible financial estimate? | Executive summary with gross/net estimate, delivery outcomes, trend, confidence, and material caveats |
| Department manager | Which workflows automate effort and improve outcomes for my teams? | Workflow and team trends, accepted outcomes, exception/risk/recovery effectiveness, and drill-down |
| Administrator | Is adoption healthy, what does AI cost, where are errors, and are policies followed? | Funnel, cost allocation, failures, data coverage, access/policy events, and methodology configuration |
| Product champion | Where is activation blocked and which capabilities are underused? | Eligible-to-active funnel, capability usage, rejection reasons, coverage gaps, and enablement prompts |

Organization RBAC and team scope apply to every view and export. Financial rate cards and
organization-wide summaries are restricted to explicitly authorized roles. Suppressed or
redacted rows must not be inferable through totals or small cohorts; the administrator
configures the minimum cohort threshold.

## 5. Required dashboard views

Every view supports current period and historical comparison, a clear last-updated time,
filters permitted by the viewer's scope, and CSV/PDF export where appropriate.

1. **Current period and historical trend:** hours returned, gross/net financial estimate,
   outcomes, AI cost, coverage, and period-over-period change without silently restating
   prior methodology.
2. **Value by capability:** attributed hours, accepted outcomes, outcome measures, cost,
   evidence state, and confidence for each capability.
3. **Value by team:** the same measures within authorized team scope, with minimum-cohort
   suppression and an “unassigned” data-quality bucket.
4. **Adoption funnel:** eligible users/workflows → activated → proposal shown → reviewed →
   accepted or edited-and-accepted → outcome completed. Each stage displays its denominator
   and drop-off reason.
5. **Accepted-versus-rejected proposals:** counts and rates by capability and team, split
   into accepted, edited-and-accepted, rejected, expired, and pending, with controlled
   rejection reasons.
6. **Cost per accepted outcome:** AI cost divided by accepted outcomes, plus total cost,
   cost per run, cost per delivered/matured outcome, and zero-denominator handling as
   unavailable.
7. **Management hours returned:** deduplicated hours from manager-role activity, broken
   down by primary value category; do not infer role from message prose.
8. **Forecast and recovery effectiveness:** forecast error and tolerance trend, early-risk
   lead time, recovery adoption, and delivery after adoption, with comparable cohorts and
   matured denominators.
9. **Data coverage and unavailable metrics:** required sources, event coverage, baseline
   status/age, permissions, cohort maturity, freshness, and the specific remediation for
   every unavailable metric.

The executive export for a quarterly business review includes reporting period, scope,
hours returned, delivery and forecast outcomes, gross and net financial estimates, AI
cost, top capabilities, coverage, confidence, methodology version, assumptions, and
unavailable metrics. It carries a generated timestamp and cannot omit methodology notes.

## 6. Data quality, governance, and operations

- Validate event schemas, stable identifiers, organization boundaries, timestamps, actor
  roles, and currency before attribution; quarantine invalid events rather than treating
  them as zero-value activity.
- Publish late-arrival and restatement rules. The default close lag is seven days; later
  events appear as a labeled restatement with an audit record.
- Version baseline, allocation, FX (if enabled), rate-card, and metric logic. Never combine
  currencies in a total without a customer-approved conversion source and effective date.
- Monitor duplicate-event rate, unmatched workflow runs, missing actor roles, stale
  baselines, missing cost allocations, and source freshness.
- Record configuration changes, exports, baseline approvals, and restatements in an audit
  trail. Customers can export their methodology and supporting aggregate evidence.
- Separate product telemetry from customer content, minimize personal data, honor
  retention/deletion policy, and exclude deleted or unauthorized content from future
  recomputation.

## 7. Pilot commercialization criteria

A pilot is eligible for commercial review only when all criteria below pass for the agreed
pilot scope and period:

| Criterion | Pass evidence |
| --- | --- |
| Customer agrees the displayed methodology is credible | Named customer business owner approves the baseline, attribution rules, rate card, evidence labels, and dashboard methodology in writing |
| At least one workflow demonstrates at least 40% effort reduction | `eligible minutes returned / baseline active minutes ≥ 40%` over the agreed representative sample, with an approved baseline and measured product events |
| Value is attributable to specific product events | Each claimed value ledger entry links to valid workflow, product, review, and outcome events; aggregate evidence coverage meets the pilot agreement |
| AI cost is visible next to delivered outcomes | Dashboard and export show total AI cost, cost per accepted outcome, and cost per delivered/matured outcome beside value and outcome measures |
| An executive summary can be exported for a quarterly business review | Authorized buyer successfully generates and reviews the versioned QBR PDF/CSV package defined above |

Passing the criteria authorizes a commercialization decision, not an automatic claim of
causality or guaranteed ROI. The closeout records sample size, comparison design,
unavailable metrics, confidence, customer approval, and any material operational or policy
exceptions. A failed criterion remains failed; it cannot be replaced by an AI narrative or
an unrelated aggregate improvement.

## 8. Acceptance checklist for implementation

- [ ] Baselines are configurable, versioned, approved, and effective-dated.
- [ ] Workflow, proposal, outcome, and AI-cost events join through stable identifiers.
- [ ] Every metric renders measured, estimated, or unavailable with coverage and notes.
- [ ] Attribution-ledger tests prove overlapping workflows do not double-count value.
- [ ] Financial estimates expose rate cards, ranges, cost, confidence, and methodology.
- [ ] All four audiences receive scoped views without bypassing organization/team RBAC.
- [ ] All nine required views support the required periods, filters, and empty states.
- [ ] QBR export reproduces dashboard totals and includes assumptions and methodology.
- [ ] Pilot criteria can be evaluated from stored evidence without manual narrative scoring.
