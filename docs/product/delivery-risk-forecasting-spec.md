# Delivery-Risk Forecasting Product and Data Specification

## 1. Purpose and decision scope

TaskMind must help delivery leaders identify committed milestones that are likely to miss
their target dates early enough to intervene. The feature is a decision-support tool, not
an automated performance-management system. It forecasts delivery outcomes from
authorized work-management evidence, explains what changed, and preserves the human
decision and intervention trail.

The first release answers one question:

> Given the evidence available at this forecast time, what is the probability that this
> committed milestone will finish after its currently committed target date?

The product must support portfolio triage while making uncertainty, source freshness,
and the evidence behind each displayed factor visible. Forecasts must not silently turn
missing evidence into either low risk or high confidence.

### Goals

1. Detect milestone misses earlier than the current management process.
2. Rank attention by expected business impact, not probability alone.
3. Make every displayed risk factor traceable to authorized underlying evidence.
4. Separate delivery risk from confidence in the available data.
5. Help managers choose, record, and evaluate interventions without changing work
   automatically.
6. Create a complete forecast-and-outcome history suitable for offline evaluation,
   calibration, audit, and model comparison.

### Non-goals

- Predicting individual employee performance, intent, attrition, or productivity.
- Ranking people or using forecasts for compensation or disciplinary decisions.
- Automatically changing scope, owners, priorities, or dates.
- Predicting cost overrun, quality, or business-value realization in the initial target.
- Treating an AI-generated explanation as the source of the risk score.
- Training an online or self-updating model during the pilot.

## 2. Users and decisions

| User                       | Primary decision                                                                      | Required controls                                                                                            |
| -------------------------- | ------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| Portfolio leader           | Which at-risk commitments need executive attention first?                             | Business-impact ranking, filters, trend, confidence, and cross-project evidence.                             |
| Project or program manager | Whether to accept, mitigate, or reject a detected risk and which intervention to try. | Factor drill-down, recommendations, feedback, intervention log, and forecast history.                        |
| Milestone owner            | What evidence or plan needs correction?                                               | Source links, freshness warnings, and ability to update the owning work record through existing workflows.   |
| Organization administrator | Whether forecasting is permitted and how it is governed.                              | Organization/project kill switches, retention, eligibility, protected-input policy, and audit access.        |
| Product/data analyst       | Whether the forecast is safe and useful enough to launch.                             | Immutable snapshots, outcome labels, baseline comparison, segmented metrics, and exportable evaluation data. |

Forecast visibility follows the permissions of the milestone, project, and cited source.
Portfolio aggregation never grants access to otherwise restricted project evidence.

## 3. Prediction target and forecast contract

### 3.1 Eligible prediction unit

One prediction unit is one **committed milestone at one forecast timestamp and one
horizon**. A milestone is eligible only when it:

- belongs to an organization and project where forecasting is enabled;
- has an explicit committed target date and a recorded commitment timestamp;
- is not completed, cancelled, or archived at the forecast cutoff;
- is within the selected forecast horizon according to the organization time zone; and
- has a stable milestone identifier and sufficient authorization to evaluate its linked
  work.

Forecast horizons are **7, 14, and 30 calendar days**. A horizon answers whether an
eligible milestone whose target date falls on or before `forecasted_at + horizon` will
miss its target. A separate forecast record is stored for each horizon. The UI may group
the three records, but it must not blend their probabilities.

### 3.2 Outcome definition

The binary target is:

```text
missed = actual_completion_at > committed_target_at_cutoff
         OR milestone remained incomplete at end_of_target_date
on_time = actual_completion_at <= committed_target_at_cutoff
```

Dates are evaluated in the organization time zone and normalized to instants for
storage. Completion at or before the end of the committed target date is on time. An
eligible milestone still incomplete after that date is missed even if it is later
rescheduled.

The label uses the target date known at the forecast cutoff. Later date changes do not
rewrite the target attached to an existing forecast. Cancellation, merging, deletion,
or a material definition change after the cutoff produces an `INDETERMINATE` outcome
with a reason rather than an on-time or missed label. Indeterminate cases are excluded
from primary precision, recall, and calibration metrics and reported separately.

### 3.3 Score, bands, and display precision

The score is the probability `p(missed)` in the closed interval `[0, 1]`. The initial
band mapping is configuration-versioned and fixed for offline evaluation:

| Risk band | Initial probability interval | Default presentation                       |
| --------- | ---------------------------- | ------------------------------------------ |
| Low       | `0.00 <= p < 0.25`           | Monitor.                                   |
| Medium    | `0.25 <= p < 0.50`           | Review during the normal delivery cadence. |
| High      | `0.50 <= p < 0.75`           | Assign and track a mitigation.             |
| Critical  | `0.75 <= p <= 1.00`          | Prompt accountable leadership review.      |

Thresholds may be changed only through a new score-policy version validated offline;
historical bands are never recomputed in place. The customer-agreed critical threshold
and false-positive tolerance are recorded in launch configuration.

Probability is distinct from business impact. Portfolio priority is ordered by an
explainable **attention priority** such as `risk probability × configured business
impact`, with deterministic tie-breakers of earlier target date and stable milestone ID.
Impact must be an explicit authorized project/milestone value or policy-derived tier,
not an inference about a person or team. Both probability and impact remain visible so
the ranking can be understood.

### 3.4 Confidence and data sufficiency

Every forecast carries a separate data-sufficiency state; it is not folded into the
risk probability:

| State        | Meaning                                                                                               | Forecast presentation                                                                                                                               |
| ------------ | ----------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| Sufficient   | Required coverage, history, and freshness gates pass.                                                 | Show probability and risk band with normal precision.                                                                                               |
| Limited      | A score can be calculated, but one or more important optional signals are missing or partially stale. | Show a rounded range or qualitative band, label **Limited evidence**, and list gaps.                                                                |
| Insufficient | Required evidence, minimum history, or freshness gates fail.                                          | Do not show a precise probability or assign a definitive risk band; show **Forecast unavailable—insufficient evidence** and the remediation needed. |

The sufficiency calculation is deterministic and versioned. At minimum it records
eligible signal count, available signal count, required-signal coverage, lookback
coverage, latest evidence time, stale signal count, inaccessible-source count, and each
failed gate. A model's statistical uncertainty may be stored separately, but it cannot
replace the data-sufficiency indicator.

## 4. Candidate signal specification

All signals are calculated **as of the forecast cutoff** using only evidence that existed
then. Each signal definition is versioned, leakage-tested, and reproducible. Windows
below are initial defaults and must be tuned only through offline evaluation.

| Signal                                              | Initial deterministic feature                                                                                                                                                                    | Availability and evidence                                                                                                                                                |
| --------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Completion velocity versus remaining scope          | Completed committed work units in the trailing 14 days divided by active days, compared with remaining eligible work units divided by days to target. Preserve numerator, denominator, and unit. | Requires linked in-scope work, stable completion events, and comparable work units; link included tasks and completion history.                                          |
| Scope growth                                        | Net and gross eligible work added in trailing 7/14/30 days divided by scope at window start. Distinguish additions, removals, and re-estimation.                                                 | Requires versioned milestone/task membership or scope events; link each scope-change event.                                                                              |
| Cycle-time drift                                    | Median completed-task cycle time in the trailing 30 days versus the prior comparable 30-day window and, when available, the project baseline.                                                    | Requires enough comparable completed tasks; link task start/completion timestamps and cohort definition.                                                                 |
| Blocker count and blocker age                       | Count of unresolved milestone-linked blockers plus maximum, median, and severity-weighted age.                                                                                                   | Requires explicit blocker status and creation/resolution history; link active blockers.                                                                                  |
| Dependency age                                      | Count and age of unresolved predecessor or external dependencies, emphasizing dependencies already past their expected resolution date.                                                          | Requires explicit dependency records and state history; link dependency and permitted upstream evidence.                                                                 |
| Repeated rescheduling                               | Number of target-date changes in trailing 30/90 days and cumulative days moved later.                                                                                                            | Requires immutable milestone schedule history; link each old/new date event and actor-visible audit record.                                                              |
| Unassigned critical work                            | Count and weighted share of incomplete critical/high-priority work without an accountable owner.                                                                                                 | Requires explicit priority/criticality and owner fields; link qualifying tasks.                                                                                          |
| Ownership churn                                     | Number of accountable-owner changes for milestone-linked work in trailing 30 days, normalized by active work count.                                                                              | Requires ownership history; link reassignment events without inferring employee traits.                                                                                  |
| Workload concentration                              | Share of remaining estimated work assigned to the most-loaded owner and concentration index across owners.                                                                                       | Requires explicit assignments and comparable work estimates; link aggregate to underlying assignments while respecting permissions.                                      |
| Reopened tasks                                      | Count and share of completed milestone-linked tasks reopened in trailing 30 days.                                                                                                                | Requires status-transition history; link reopen events.                                                                                                                  |
| Missed interim milestones                           | Count and recency of linked predecessor/checkpoint misses before the committed milestone.                                                                                                        | Requires committed interim dates and immutable outcomes; link each missed checkpoint.                                                                                    |
| Stale status updates                                | Age of latest material milestone update and share of active linked work beyond the configured update threshold.                                                                                  | Requires update timestamps and threshold policy; link stale work records. Absence is unknown, not lack of progress.                                                      |
| Historical delivery performance for comparable work | Smoothed miss rate for a declared comparison cohort such as project type, work type, size bucket, and horizon.                                                                                   | Requires adequate cohort size and outcome history; show cohort definition/count and suppress sparse cohorts. Never use protected-attribute proxies to construct cohorts. |

Signal values must retain their raw measurement, normalized value if used by a score,
lookback window, expected direction, source timestamp range, availability state, stale
state, source references, and calculation version. Estimates are never silently mixed
with task counts; if scope units are inconsistent, the affected feature is unavailable.

### 4.1 Signal availability states

Each candidate signal has one of these states per forecast:

- `AVAILABLE`: computable, authorized, and within freshness policy;
- `STALE`: computable, but its newest required evidence is older than policy allows;
- `PARTIAL`: some permitted evidence exists but coverage is incomplete;
- `MISSING`: required records or history do not exist;
- `INACCESSIBLE`: relevant records exist but cannot be used under access or residency
  policy; or
- `NOT_APPLICABLE`: the definition does not apply to this milestone.

The product displays the state of every candidate signal. `MISSING`, `INACCESSIBLE`, and
`NOT_APPLICABLE` values are not coerced to zero. Stale signals may be shown as evidence
but cannot satisfy a freshness gate.

## 5. Deterministic baseline

Before any predictive model is evaluated, Relay must implement and version a simple,
auditable rules baseline. The initial baseline uses only available, non-stale signals and
fixed points:

| Rule                                                                            | Points |
| ------------------------------------------------------------------------------- | -----: |
| Required velocity is greater than 1.25 times recent completion velocity         |     25 |
| Scope grew by at least 10% in the trailing 14 days                              |     10 |
| Median cycle time worsened by at least 20% from the prior comparable window     |     10 |
| Any unresolved critical blocker, or oldest blocker is at least 7 days old       |     15 |
| Any overdue unresolved dependency, or oldest dependency is at least 14 days old |     10 |
| Target moved later at least twice in 30 days                                    |     10 |
| Any unassigned critical work                                                    |     10 |
| Any missed interim milestone in 30 days                                         |     10 |

Points are capped at 100 and mapped to a baseline probability using a fixed, documented
lookup table learned only from the training period; until that table exists, points are
reported as a rules score rather than mislabeled as probability. Rules that cannot be
evaluated contribute neither zero nor points and reduce sufficiency. Baseline output
includes fired rules and linked evidence.

The baseline is frozen before model comparison. Candidate models must use identical
eligible cohorts, cutoff timestamps, labels, and evaluation splits. The baseline remains
available after launch for monitoring and rollback.

## 6. Data contract and retention

### 6.1 Forecast snapshot

Every forecast run stores an immutable snapshot with at least:

```text
forecast_id, organization_id, project_id, milestone_id
forecasted_at, horizon_days, committed_target_at_cutoff, organization_timezone
eligibility_policy_version, feature_definition_version, score_policy_version
scorer_type (RULE_BASELINE | MODEL), scorer_version
probability_missed (nullable), risk_band (nullable)
data_sufficiency_state, sufficiency_metrics, failed_sufficiency_gates
attention_priority, business_impact_value, business_impact_source
available_signal_count, candidate_signal_count, latest_evidence_at
projection_status (CURRENT | STALE | SUPERSEDED)
feature_snapshot[], explanation_status, created_at
```

Each `feature_snapshot` contains signal key/version, raw and normalized values, unit,
window, availability, freshness, evidence cutoff, evidence references, rule/model
contribution, and a display-safe evidence summary. The model artifact identifier and
inference configuration are retained for reproducibility.

A forecast is marked `STALE` when the configured refresh interval has elapsed, material
source data changes, the committed target changes, the milestone closes, or the scorer
or eligibility policy is withdrawn. Staleness never deletes or rewrites the snapshot.
The UI shows both `forecasted_at` and the material change that made it stale.

### 6.2 Material change and trend record

A material change event is recorded when any of the following occurs:

- the risk band changes;
- probability moves by at least 10 percentage points;
- sufficiency changes between sufficient, limited, and insufficient;
- a top-three contributing signal changes;
- business impact changes enough to alter its priority tier; or
- a manager records or changes a disposition or intervention.

The threshold is policy-versioned. Trend views use stored forecasts, not a reconstructed
current-state history, and annotate the first forecast after each material change.

### 6.3 Outcome and feedback records

After the target date or terminal milestone event, an outcome resolver appends:

```text
forecast_id, outcome (MISSED | ON_TIME | INDETERMINATE)
actual_completion_at, outcome_resolved_at, outcome_reason
target_date_change_after_cutoff, cancellation_or_merge_reference
label_policy_version
```

Manager feedback is a separate, append-only record containing disposition
(`ACCEPTED_RISK`, `MITIGATED`, or `INCORRECTLY_DETECTED`), reason code, optional note,
actor, time, and forecast ID. `INCORRECTLY_DETECTED` is user feedback, not the ground
truth label; evaluation still uses the eventual milestone outcome.

An intervention record includes type, description, responsible owner, planned and
completed times, related evidence/work references, originating forecast, status, and
author. Subsequent forecasts link active interventions so the product can display what
changed after an intervention without claiming causality.

### 6.4 Storage and ownership

- Core owns milestone commitments, permissions, manager dispositions, intervention
  workflow, organization/project enablement, and customer-facing facade endpoints.
- Relay owns deterministic feature calculation, forecast snapshots, baseline/model
  scoring, trend projection, outcome joins, and evaluation datasets.
- Nova owns generated explanations and intervention proposals, keyed to an immutable
  Relay forecast and its evidence manifest.
- The frontend calls Core only. Core returns only evidence the caller is authorized to
  view and obtains internal analytics/explanations from Relay and Nova.

Retention is organization-configurable subject to the evaluation and audit policy.
Deletion and residency requests must propagate to evidence copies and evaluation
exports. Aggregate evaluation data must meet minimum cohort-size rules before display.

## 7. Data quality, privacy, and safety

### 7.1 Quality gates

The versioned sufficiency policy must define, at minimum:

- minimum milestone and linked-work history;
- required availability for target date, scope, status history, and outcome labeling;
- minimum number of applicable available signals;
- per-signal freshness thresholds and forecast refresh interval;
- minimum comparable-work cohort size;
- handling for inconsistent estimates, duplicate events, late-arriving events, and time
  zone changes; and
- behavior when source permissions remove part of the evidence.

No precise forecast is produced when required gates fail. The user sees available,
partial, stale, missing, inaccessible, and not-applicable signals plus actions that could
improve sufficiency. Data-quality monitoring tracks coverage, freshness, invalid events,
late arrivals, feature drift, outcome-label delay, and forecast-to-outcome join rate.

### 7.2 Protected-input exclusion

Prediction features, cohort definitions, embeddings, prompts, and free-text-derived
features must not include protected personal attributes or known proxies. At minimum,
exclude race/ethnicity, color, nationality or national origin, citizenship, religion,
sex, gender identity, sexual orientation, pregnancy, marital/family status, age, genetic
information, disability or health information, veteran status, and other attributes
protected by applicable customer jurisdiction.

The initial release does not derive predictive features from free-form text. Person IDs
may be used transiently to calculate workload concentration or ownership churn, then
replaced with aggregate values; identities are not model inputs. Team size may be used
for evaluation segmentation and a reviewed operational feature only if approved, but
never as a proxy for a protected class.

An allowlist controls production features. Schema checks reject unknown fields; feature
lineage records source fields; pre-release reviews test correlation and proxy risk; and
access logs audit every evaluation export. Sparse segments are suppressed. Customers
must be told that the feature forecasts work delivery, not people.

## 8. User experience

### 8.1 Portfolio risk view

The default portfolio table ranks eligible milestones by attention priority and shows:

- milestone, project, accountable owner, committed target, and horizon;
- risk band and probability only when permitted by sufficiency;
- separate data-sufficiency state and evidence-coverage summary;
- explicit business-impact value/tier and why it affects rank;
- trend direction, latest forecast time, and latest material-change date;
- top contributing signal summary;
- stale/disabled states; and
- manager disposition and active intervention status.

Users can filter by horizon, risk band, sufficiency, impact, project type, team, target
window, disposition, intervention status, and stale state. Disabled projects are omitted
from scoring and clearly identified to authorized administrators rather than appearing
as low risk.

### 8.2 Milestone risk detail

The detail view must provide:

1. A risk history chart for each horizon with band thresholds, sufficiency, forecast
   timestamps, and annotated material changes.
2. The top positive and negative contributing signals, expressed in plain language and
   never as unsupported causal claims.
3. An all-signals panel showing availability, value, window, freshness, contribution,
   and missing-data remediation.
4. An evidence drawer for every displayed factor, linking to the underlying permitted
   tasks, blockers, dependencies, schedule changes, and metric cohort. If evidence is no
   longer accessible, show that state instead of a cached sensitive detail.
5. Deterministic and Nova-drafted recommended interventions, each labeled as a proposal
   with rationale, evidence, responsible role, and expected review date.
6. Controls to record `Accepted risk`, `Mitigated`, or `Incorrectly detected`, with a
   reason and optional note.
7. An intervention timeline alongside subsequent forecasts, highlighting association
   without claiming that an intervention caused a score change.

Low data quality is shown beside the headline and again in the explanation; it cannot be
hidden in a tooltip. A stale projection carries a persistent banner with the forecast
date, stale-since date, reason, and next refresh state.

### 8.3 Notifications and interventions

Critical/high alerts are deduplicated by milestone, horizon, band, and material-change
event. A notification identifies the material change, sufficiency, top evidence-backed
factors, and review link. Reminder cadence is configurable to limit warning fatigue.

Recording a disposition does not suppress future material deterioration. An accepted or
incorrectly detected warning can be snoozed under policy, but a new critical transition
or material evidence change reopens review. No recommendation executes automatically;
authorized users use existing TaskMind workflows to change scope, owners, priorities,
or dates.

## 9. AI boundaries and explanation contract

Relay, or a successor deterministic analytics service, calculates risk probability,
band, sufficiency, contributions, trend, and evidence manifest. Nova does not calculate,
adjust, or override them. Nova receives a structured, immutable forecast payload rather
than unrestricted project context and may:

- explain the highest contributing and mitigating factors;
- summarize material changes since the prior forecast; and
- draft evidence-grounded intervention proposals.

Nova output must use only supplied facts, preserve all numbers/dates/directions, mention
limited or insufficient evidence prominently, and distinguish correlation from known
causation. It must not introduce a factor absent from the manifest, contradict a signal
direction, claim certainty, infer personal traits, or describe a proposal as completed.

Core/Nova validate generated output against the structured forecast. Numeric and entity
references must match the manifest; displayed factors must have evidence IDs; prohibited
actions and unsupported claims cause rejection. On validation failure or Nova outage,
the product renders a deterministic explanation template and evidence-backed
intervention checklist. The calculated forecast remains available.

Explanations are regenerated only for a specific forecast version. They carry generation
time, prompt/template version, model identifier, validation result, and an
**AI-generated explanation** label. Recommendations require explicit human action and
cannot mutate TaskMind records by tool call or side effect.

## 10. Offline evaluation plan

No customer sees forecasts until a retrospective, time-correct offline evaluation has
passed. The evaluation must:

1. Freeze eligibility, feature, label, baseline, and band policies.
2. Reconstruct features using only events available at each historical cutoff to prevent
   future leakage.
3. Split training, calibration, and test data by time, with organizations/projects kept
   isolated where needed to avoid entity leakage.
4. Evaluate each 7-, 14-, and 30-day horizon separately on the same eligible examples as
   the deterministic baseline.
5. Publish overall and segmented results, uncertainty intervals, exclusions, and data
   sufficiency coverage.

### 10.1 Required metrics

| Measure                         | Requirement                                                                                                                                                   |
| ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Precision for missed milestones | Report by horizon and high/critical operating threshold: `TP / (TP + FP)`.                                                                                    |
| Recall for missed milestones    | Report by horizon and high/critical operating threshold: `TP / (TP + FN)`.                                                                                    |
| Calibration                     | Report Brier score, calibration curve/reliability table, expected calibration error, and observed miss rate by probability bucket.                            |
| Baseline comparison             | Compare precision, recall, precision-recall AUC, Brier score, and lead time against the frozen rules baseline using paired cohorts.                           |
| Segmented accuracy              | Report all primary metrics by configured project type and team-size bucket; suppress groups below the privacy/reliability minimum.                            |
| False positives                 | Track warnings whose labeled outcome is on time, including critical-risk false-positive rate and user `INCORRECTLY_DETECTED` feedback separately.             |
| Ignored warnings                | Track high/critical warnings with no disposition or intervention before the target date; report outcome and lead time without assuming why they were ignored. |
| Early detection                 | For each missed milestone, compare the first qualifying high/critical forecast with the first equivalent risk recorded by the current management process.     |

Evaluation also reports eligibility rate, insufficient/limited/sufficient shares, outcome
coverage, indeterminate rate, alert frequency, repeat alerts, model/baseline disagreement,
and performance drift. Missing manager feedback is not interpreted as acceptance.

### 10.2 Current-process comparator

Before the pilot, each customer defines the observable current-process marker used for
lead-time comparison, such as the first red status, formal risk-log entry, escalation, or
target-date change. The definition is fixed for that evaluation period. If no reliable
marker exists, early-detection performance is reported as unmeasured rather than
estimated.

## 11. Launch gates and rollout controls

All gates must pass on a held-out offline test set before customer exposure:

1. **Baseline improvement:** the candidate model materially outperforms the deterministic
   baseline on pre-agreed primary metrics at the chosen operating thresholds, with no
   material calibration regression. “Materially” must be quantified with each pilot
   customer before evaluation (for example, minimum absolute recall gain at a fixed
   precision).
2. **Critical false positives:** critical-risk false-positive rate is at or below the
   customer-agreed tolerance, reported with a confidence interval and minimum sample
   requirement.
3. **Evidence completeness:** 100% of displayed risk factors have at least one valid,
   permission-checked evidence reference. Any failed reference suppresses that factor
   and fails the release audit if it was displayed.
4. **Seven-day early warning:** the system detects a customer-agreed meaningful portion
   of eventual misses at least seven days before the current-process marker, using only
   milestones for which that comparator is measurable.
5. **Operational controls:** forecasting can be disabled independently at organization
   and project level. Disablement stops new scoring and notifications immediately;
   authorized retention/audit access to historical forecasts follows policy.

Rollout proceeds from internal dogfood to offline customer shadow mode, then an
administrator-enabled pilot. Shadow forecasts are not shown to ordinary customers and
cannot send alerts. Promotion requires an explicit review by product, data science,
security/privacy, and customer success. Rollback selects the deterministic baseline or
disables forecasts without deleting historical evaluation records.

## 12. Functional acceptance criteria

- A forecast is stored separately for every eligible milestone/horizon/cutoff and later
  joins to an immutable eventual outcome.
- Risk probability/band and data sufficiency are separately calculated and displayed.
- Insufficient evidence never produces or displays a precise probability.
- Every candidate signal displays its availability and freshness state.
- Every displayed contributor links to permission-checked underlying evidence.
- The portfolio orders risk by explicit business impact and exposes the ranking inputs.
- Trend history identifies material-change dates and never reconstructs old predictions
  from current data.
- Managers can append dispositions and interventions, and see subsequent forecasts.
- Relay calculates the forecast; Nova only explains evidence and drafts proposals.
- Generated narratives prominently disclose low data quality and pass contradiction and
  evidence validation, or fall back to deterministic copy.
- The frozen rules baseline and candidate model are evaluated offline for precision,
  recall, calibration, segmented performance, false positives, ignored warnings, and
  early-detection lead time.
- Organization and project disablement is authorization-checked, audited, and effective
  before another forecast or alert is created.
- Protected attributes, free-text-derived personal traits, and unapproved features are
  rejected from scoring inputs.

## 13. Open decisions before implementation

1. Which milestone field or workflow constitutes an explicit commitment, and can a
   project maintain both desired and committed dates?
2. What minimum history, signal coverage, freshness thresholds, and comparable-cohort
   sizes define sufficient versus limited evidence?
3. Which scope unit is authoritative when task estimates are absent or heterogeneous?
4. How is business impact configured, audited, and normalized across portfolios?
5. What probability-band and material-change thresholds meet each pilot customer's
   operating tolerance?
6. Which current-process event is reliable enough to measure seven-day improvement?
7. What retention, residency, and explanation-model policies apply per customer?
8. Which intervention taxonomy supports useful measurement without implying causal
   effectiveness?
9. What quantitative margin constitutes material baseline outperformance for launch?
10. Which project-type and team-size taxonomies have enough stable, privacy-safe data for
    segmented evaluation?
