# Enterprise AI Governance Specification

**Status:** Product and architecture specification  
**Audience:** Organization administrators, security and compliance reviewers, managers,
end users, and TaskMind implementers  
**Service ownership:** Core authorizes users, approvals, and business-state mutations;
Nova evaluates AI policy, invokes providers and tools, and records AI runs; Relay supplies
permission-filtered read context; the frontend presents Core facades only.

## 1. Purpose and principles

This specification defines the controls, records, and user experiences required to use
TaskMind AI in an enterprise organization. It applies to every AI invocation, including
interactive generation, background generation, agent tool use, proposed mutations,
regeneration, and retries.

The governance model follows these principles:

1. **Deny by default.** A run proceeds only when its capability, provider/model, data,
   context sources, tools, geography, contract, budget, and rate limits are all allowed.
2. **Enforce on the server.** The frontend may explain or preview policy, but Core and
   Nova make authoritative decisions for every request and every tool call.
3. **Preserve human control.** AI output is a proposal until an authorized human applies
   it. Configured approvals cannot be inferred from generation, tool access, or the
   requester's role.
4. **Minimize data.** Send only authorized, necessary context to a provider. Secrets,
   credentials, hidden prompts, and unrelated sensitive data are never exposed in user
   output or compliance exports.
5. **Make use accountable.** Every attempted run receives an audit record, including
   denied and failed runs, with enough metadata to reconstruct policy and cost outcomes.
6. **Separate duties.** Administrators configure controls, security reviewers inspect
   evidence, managers manage delegated budgets and approvals, and end users initiate and
   review permitted work. Assignment of multiple roles never implicitly waives a control.

Normative terms **MUST**, **MUST NOT**, **SHOULD**, and **MAY** have their usual policy
meaning. An organization is the tenant boundary. A team is an organization-scoped group.

## 2. Roles and responsibilities

| Role | Responsibilities | Explicit limitations |
| --- | --- | --- |
| Organization administrator | Configure and activate policy, assign policy-management roles, set budgets and alerts, disable capabilities, and export authorized evidence. | Cannot reveal provider credentials or unredacted hidden prompts; cannot approve their own high-impact proposal when separation of duties is required. |
| Security/compliance reviewer | Read effective policy, audit runs, policy history, retention evidence, and compliance exports; attest or flag findings. | Read-only by default; cannot enable a capability, increase a budget, or apply a proposal without a separately granted role. |
| Manager/team owner | Allocate a team budget within the organization ceiling, receive alerts, review team-level usage, and approve configured proposals for their scope. | Cannot weaken organization policy, view another team's prompt content, or exceed organization ceilings. |
| End user | Invoke enabled capabilities, inspect provenance and warnings, review proposals, report incorrect output, and view their own run metadata. | Cannot select a disallowed provider/model, add unauthorized context or tools, bypass approvals, or retrieve hidden prompts and secrets. |
| Service operator | Operate infrastructure, respond to incidents, and invoke an audited emergency disable control when authorized. | Infrastructure access alone does not grant organization-data, prompt-content, policy-administration, or proposal-approval access. |

All administrative and reviewer operations require explicit organization-scoped RBAC.
Sensitive operations SHOULD support just-in-time access and MUST be included in the
organization audit trail.

## 3. Policy model

### 3.1 Policy objects, lifecycle, and precedence

An organization has one active, immutable-versioned `AiGovernancePolicy`. Draft changes
are validated before activation. Activation creates a new version with author, timestamp,
reason, normalized diff, and optional change-ticket reference. Previous versions remain
readable for the audit-retention period and cannot be edited.

The effective decision combines these layers, from strongest to weakest:

1. TaskMind safety and contractual hard stops;
2. emergency global or organization capability disablement;
3. organization policy;
4. sensitive-project classification and project overrides that only narrow access;
5. team policy and budget, which only narrow organization policy;
6. user entitlements and budget, which only narrow team policy;
7. capability defaults.

At every layer, explicit deny overrides allow. A child scope MUST NOT expand its parent.
Policy changes apply to new runs immediately after activation without a service redeploy.
Long-running runs MUST re-evaluate policy before each provider invocation, tool call, and
mutation; a newly applicable denial stops safely. Each decision records the active policy
version and a stable reason code.

### 3.2 Organization policy controls

#### Enabled AI capabilities

Administrators configure each registered capability as `DISABLED`, `ENABLED`, or
`ENABLED_WITH_APPROVAL`, optionally narrowed by team, role, project classification, and
environment. Unknown capabilities are disabled. A capability definition declares its
data classes, context sources, tool categories, mutation impact, and whether it can run
in the background. Policy activation fails if a capability references an unavailable
provider/model or omits a required approver.

Capability disablement blocks new runs and provider retries. Administrators choose
whether active runs are allowed to finish read-only work or are cancelled immediately;
no active run may perform a mutation after disablement.

#### Allowed providers and models

The policy allowlists provider and model identifiers; display-name matching or an
unbounded `latest` alias is insufficient. Each entry includes:

- approved capabilities and data classifications;
- approved model versions or a controlled alias whose resolved model is audited;
- provider data-use/training terms, zero-retention eligibility, and contract reference;
- region/data-residency route and cross-border-transfer basis;
- maximum context/output size and optional per-run cost ceiling;
- activation and review/expiry dates; and
- fallback order.

Fallback is never implicit: the fallback provider/model MUST independently pass the full
policy evaluation. Provider credentials remain in the approved secret store and are
never policy fields, audit payloads, or client responses.

#### Sensitive projects and external providers

Projects carry an organization-defined data classification, including at minimum
`STANDARD`, `CONFIDENTIAL`, and `RESTRICTED`. Administrators can mark a project
`externalProviderExcluded`. Context derived from such a project MUST NOT be sent to an
external provider, including through embeddings, fallback, tool results, attachments,
cached prompts, or retries.

When a request combines projects, the strictest classification applies. The system may
route to an explicitly approved private/internal model or decline the run with a safe
reason. It MUST NOT silently omit material restricted context and present the answer as
complete. Declassification requires an authorized, audited project-classification
change; copying text into another project does not remove its source classification.

#### Data retention and prompt/response storage

Retention is configured by artifact class: audit metadata, prompt content, response
content, context snapshots, tool inputs/outputs, proposals, user feedback, cost ledger,
and compliance exports. Each has a duration, legal-hold behavior, deletion method, and
minimum/maximum bounds imposed by contract and law.

Prompt and response storage is independently configurable as:

- `NONE`: retain hashes, sizes, classifications, and audit metadata only;
- `REDACTED`: retain policy-redacted content with redaction version and integrity hash;
- `ENCRYPTED_FULL`: retain encrypted content only when explicitly permitted and access
  controlled; or
- `TRANSIENT`: keep encrypted content only for the bounded duration required to complete
  the run, then delete it.

`ENCRYPTED_FULL` is not the default. Secrets and credentials are removed before any
storage mode. Search indexes and observability systems MUST NOT become shadow prompt
stores. Expiry jobs produce deletion evidence containing counts, artifact types, policy
version, cutoff, completion time, and failures, without reproducing deleted content.
Legal holds are scoped, authorized, time-bounded where possible, and audited.

#### Permitted context sources

The policy allowlists source categories and, where appropriate, specific connections:
TaskMind tasks/projects, Relay projections, user-selected attachments, approved wiki or
document repositories, approved issue trackers, calendars, and chat systems. Each source
declares permitted classifications, capabilities, teams/projects, freshness bounds, and
whether retrieval snippets may leave the approved processing region.

Context access is evaluated using the requesting user's current permissions at retrieval
time and again before provider submission. The run records identifiers, versions, and
classifications—not unauthorized source bodies—in its audit metadata. Revoked or stale
access fails closed. User-provided text cannot cause retrieval from an unapproved source.

#### Permitted tool categories

Tools are registered in categories with declared input/output schemas and impact:

| Category | Examples | Default posture |
| --- | --- | --- |
| `READ_CONTEXT` | Search permitted tasks, fetch project status | Allow only within user permissions and source policy. |
| `ANALYZE` | Calculate duration or summarize authorized evidence | Allow when the capability and data path are approved. |
| `DRAFT` | Create a non-applied task/report proposal | Allow with visible provenance; no business-state change. |
| `MUTATE_LOW_IMPACT` | Edit a user's own draft or preference | Require preview and confirmation unless policy is stricter. |
| `MUTATE_HIGH_IMPACT` | Bulk update, assignment, deletion, schedule change, publish/send, permission or integration change | Require configured approval and exact-version binding. |
| `EXTERNAL_COMMUNICATION` | Send email/chat, publish a report, create an external issue | Require recipient/channel preview and configured approval. |
| `ADMINISTRATIVE` | Change policy, identity, permissions, secrets, billing, or audit settings | Not callable by AI. |

Allowlisting a category does not allow every tool in it. Policy may further allowlist
tool IDs. Nova can request tools only through authenticated internal Core/Relay APIs; it
does not receive database, provider credential, browser, or arbitrary network access.
Core reauthorizes every tool invocation independently of Nova's plan.

#### Actions requiring approval

Policy maps action type and impact to `NONE`, `REQUESTER_CONFIRMATION`,
`MANAGER_APPROVAL`, `DESIGNATED_APPROVER`, `TWO_PERSON`, or `SECURITY_APPROVAL`.
High-impact mutations and external communications MUST have at least one approver other
than the model; organizations may require separation of duties so the requester cannot
approve. Permission, identity, policy, secret, billing, and audit-retention changes MUST
NOT be applied through an AI tool at all.

An approval binds the proposal ID and version, canonical mutation payload hash,
human-readable diff, target resources, policy version, evidence snapshot, warnings, and
expiry. Any material edit, regenerated output, target-state version conflict, permission
change, or policy change invalidates approval and requires re-evaluation. Expiry,
rejection, and partial execution never imply approval. Core is the final mutation gate
and records both the decision and authenticated approving user.

#### Per-user and per-team budgets

Policies define currency-denominated and token/request budgets for organization, team,
user, and capability scopes over daily and monthly periods. The effective allowance is
the minimum remaining allowance across all applicable scopes. Team managers may allocate
within, but never increase, the organization ceiling. Shared membership does not multiply
a user's allowance.

Before provider invocation, Nova reserves estimated cost against an atomic ledger. It
reconciles the reservation with provider-reported or deterministically estimated usage
and releases unused value. Retries and tool-mediated provider calls count toward usage.
At configurable percentages, the system emits manager/admin alerts. A hard threshold
denies new runs with a stable reason; a soft threshold warns but does not deny. Budget
overrides require authorized, expiring, reasoned, audited changes and never override a
provider, geography, or sensitive-project denial.

#### Rate limits

Rate limits use atomic server-side counters and may be set per organization, team, user,
capability, provider, and model for requests, concurrent runs, tokens, and tool calls.
Burst and sustained windows are independently configurable. The strictest applicable
limit wins. Rejected requests return a stable public error and safe retry time; client
headers are informational and cannot select the accounting identity. Administrators can
see aggregated throttling trends but not use rate limits to infer unauthorized content.

#### Geographic and contractual restrictions

Policy records approved processing and storage regions, residency commitments, prohibited
transfer destinations, provider contract/DPA identifiers and expiry, subprocessor status,
customer-data-training prohibition, and required zero-retention modes. The evaluated data
path includes provider inference, embeddings, safety services, caches, telemetry, support
access, backups, and disaster recovery.

A run is denied when no route satisfies all organization, project, user, provider, and
contract requirements. Contract expiry or provider compliance-status withdrawal disables
the affected route immediately. A region label supplied by the client is never trusted;
the server resolves the actual configured endpoint and records it.

### 3.3 Policy decision contract

Evaluation returns `ALLOW`, `ALLOW_WITH_APPROVAL`, or `DENY`, plus policy version,
evaluated scope IDs, obligations, and stable reason codes such as
`CAPABILITY_DISABLED`, `MODEL_NOT_ALLOWED`, `EXTERNAL_PROVIDER_EXCLUDED`,
`CONTEXT_SOURCE_NOT_ALLOWED`, `TOOL_NOT_ALLOWED`, `APPROVAL_REQUIRED`,
`BUDGET_EXCEEDED`, `RATE_LIMITED`, `REGION_NOT_ALLOWED`, and `CONTRACT_EXPIRED`.

Decisions occur at request admission, before context retrieval, before each provider call,
before each tool call, and immediately before mutation. A denial is itself audited. Error
messages explain corrective action without disclosing hidden policy details, other users'
usage, sensitive classification rules, internal prompts, or credentials.

## 4. AI audit record

Nova writes an append-only audit record for every attempted run. Associated proposal and
Core mutation events are linked by organization, correlation, run, and proposal IDs. The
record uses structured fields rather than free-form logs and is encrypted in transit and
at rest. Access and exports are themselves audited.

| Field | Required content |
| --- | --- |
| Identity and scope | Audit-record ID, organization ID, team ID when applicable, requesting user ID (stable internal ID, not email), project IDs/classifications, and initiating channel. |
| Capability | Stable capability ID and capability-definition version. |
| Correlation and run identifiers | Correlation ID propagated across Core/Relay/Nova, unique run ID, parent run ID for retries/sub-runs, and idempotency key hash when present. |
| Timestamp | Requested, started, completed timestamps in UTC and trusted server clock source. |
| Provider and model | Provider ID, requested and resolved model IDs/versions, endpoint region, routing reason, and approved fallback chain actually attempted. Never credentials or provider authorization headers. |
| Prompt-template version | Stable template ID/version and integrity hash; not the hidden template text. |
| Context sources | Source type, opaque source/connection/resource identifiers, version/freshness, classification, permission-decision reference, and inclusion/exclusion result. Content is governed separately by storage policy. |
| Tool calls | Ordered tool-call ID, registered tool ID/version/category, sanitized argument/result hashes or redacted summaries, authorization decision, approval reference, start/end time, and outcome. |
| Validation results | Input, output-schema, safety, citation/evidence, data-loss-prevention, and mutation validation rule versions, pass/fail/warn result, and stable finding codes. |
| Policy decisions | Evaluation point, decision, policy version, scope IDs, stable reason codes, obligations, exemption/override ID, and evaluator version. |
| Proposal decision | Proposal ID/version/hash, required approval mode, `PENDING`/`APPROVED`/`REJECTED`/`EXPIRED`/`INVALIDATED` decision, decision timestamp/reason, and approving user's stable ID. |
| Performance and usage | End-to-end and provider latency, queue time, input/output/cached/reasoning tokens when supplied, request count, and retry count. Unknown values remain null rather than zero. |
| Estimated cost | Amount, ISO currency, price-card/version, estimation method, reservation/reconciliation status, and contributing provider/tool usage. |
| Final outcome | `SUCCEEDED`, `DENIED`, `AWAITING_APPROVAL`, `REJECTED`, `CANCELLED`, `PARTIAL`, or `FAILED`, plus whether any mutation was committed and the resulting Core event IDs. |
| Failure classification | Stable category (`POLICY`, `AUTHORIZATION`, `VALIDATION`, `PROVIDER`, `TIMEOUT`, `RATE_LIMIT`, `BUDGET`, `DEPENDENCY`, `CONFLICT`, `CANCELLED`, or `INTERNAL`), safe failure code, retryability, failed stage, and sanitized diagnostic reference. |
| Retention and integrity | Artifact storage modes, expiry timestamps, legal-hold reference, record schema version, previous/hash-chain or equivalent tamper-evidence field, and redaction version. |

Missing telemetry MUST be represented explicitly. Provider error bodies, stack traces,
raw authorization tokens, secrets, hidden prompts, and unrestricted context are prohibited
in the audit record. Content stored under `REDACTED` or `ENCRYPTED_FULL` is held in a
separately access-controlled artifact store and referenced by opaque ID.

Cost attribution MUST support aggregation by organization, team, capability, provider,
and resolved model without using high-cardinality monitoring tags. The durable usage
ledger is authoritative; operational metrics use bounded labels.

## 5. Administrator and reviewer experiences

All administration screens use Core facades, require organization-scoped authorization,
show the effective policy version and last refresh, and make stale state explicit.

### 5.1 Capability enablement matrix

- Rows are registered capabilities; columns show organization default, scoped overrides,
  provider/model route, permitted data classes, required approval, and current status.
- Filters cover team, project classification, user role, and environment.
- The edit flow previews affected users/projects and validation conflicts before creating
  a draft. Activation requires a reason and shows a normalized diff.
- A simulation action evaluates representative metadata without invoking a model or
  exposing source content.

### 5.2 Provider and model policy

- Display approval status, exact model/version, capabilities, classifications, region,
  contract and review expiry, retention/training terms, fallback order, and health.
- Never display credentials; show only credential presence, owner, and last rotation time
  when the viewer is authorized.
- Prevent activation of invalid routes and visibly distinguish unavailable, contract-
  expired, emergency-disabled, and merely unhealthy providers.

### 5.3 Budget thresholds and alerts

- Configure organization ceilings and delegated team/user/capability limits for daily and
  monthly periods, with soft/hard thresholds and notification recipients.
- Forecast spend from the durable ledger, distinguish estimated from provider-confirmed
  cost, and surface late adjustments.
- Alert events are deduplicated, acknowledged, and audited. Alerts never increase or
  bypass a hard limit.

### 5.4 Usage and cost dashboard

- Show runs, success/denial/failure rates, tokens, latency, estimated/confirmed cost,
  approvals, and throttling over selectable time ranges.
- Support drill-down and export by organization, team, capability, provider, and model;
  include `unknown/unpriced` buckets rather than silently dropping data.
- Apply RBAC to every aggregation and suppress small cohorts where required for privacy.

### 5.5 Searchable AI-run audit

- Search by date, run/correlation ID, user (when authorized), team, project, capability,
  provider/model, policy reason, outcome, failure class, proposal decision, and tool ID.
- A run timeline shows admission, context, provider, validation, tool, approval, and final
  mutation events with UTC timestamps.
- Default views expose metadata only. Viewing retained prompt/response content requires a
  separate permission, a reason, and an audited access event; policies may prohibit it.

### 5.6 Policy-change history

- Show immutable versions, actor, activation time, reason/ticket, reviewer when required,
  exact normalized diff, validation results, and affected scopes.
- Rollback creates and validates a new version; it never deletes history.
- Link every run to the version evaluated at each decision point.

### 5.7 Evidence export

- Authorized reviewers select time range, scopes, record classes, and an approved export
  purpose. Exports are asynchronous, encrypted, access-controlled, expiring, and audited.
- The manifest includes policy versions/diffs, sanitized run metadata, approval events,
  usage/cost summaries, retention/deletion job evidence, legal-hold metadata, export
  schema version, generation time, filters, omitted-field reasons, counts, and checksums.
- Export generation applies field-level authorization and deterministic redaction. It
  excludes credentials, tokens, secrets, hidden/system prompts, raw provider errors,
  unnecessary personal data, and prompt/response content unless explicitly selected,
  permitted by policy, and necessary for the review.

### 5.8 Emergency capability disablement

- A prominently separated control disables one capability, one provider/model route, or
  all AI for the organization without deployment. It requires reauthentication, scope,
  reason, and confirmation; two-person activation MAY be configured.
- The authoritative server-side switch takes effect immediately, blocks new work, stops
  mutation stages, invalidates pending provider retries, and signals active runs to
  cancel safely. Approval already granted does not override the switch.
- Recovery is an explicit, separately audited action after policy and health validation;
  elapsed time, actor, affected runs, and notifications form incident evidence.

## 6. End-user transparency

Every AI surface follows these rules:

1. **Generated-content marking.** Drafts and suggestions carry a persistent “AI-generated”
   label through review, copying, and proposal views. Once applied, provenance remains in
   history even if the main UI presents the resulting business object normally.
2. **Organization-data disclosure.** The UI states whether organization data was used and
   lists safe source labels/categories and freshness. It does not reveal inaccessible
   resource names or excerpts. If material sources were excluded, the limitation is shown.
3. **Confidence and warnings.** Show calibrated confidence only when the capability has a
   defined, validated measure; otherwise say that confidence is unavailable. Display
   relevant stale-source, missing-evidence, validation, policy, and partial-result warnings
   next to the output, never solely by color.
4. **Incorrect-output reporting.** A visible action records run ID, selected reason,
   optional user comment, affected output segment, and consented diagnostic metadata. It
   does not automatically store prohibited content. Reporting never applies the output.
5. **Mutation review.** Before application, show an exact human-readable diff, affected
   objects, validation warnings, tool/action list, side effects, recipients, reversibility,
   and required approvers. Users can accept only when policy permits; reject, edit, or
   regenerate creates the appropriate new proposal/version and may invalidate approval.
6. **Safe support reference.** Errors and run details show a copyable support reference
   derived from the correlation/run ID plus a stable public error code. They MUST NOT show
   internal prompts, chain-of-thought, secrets, provider credentials, raw provider error
   bodies, stack traces, internal hostnames, or unauthorized policy/context details.

Denials explain the controlling category and an appropriate next step, such as selecting
an approved project or contacting an administrator. The frontend never offers a control
that suggests a forbidden action can be forced through.

## 7. Enforcement architecture

1. The authenticated frontend submits a capability request only to Core.
2. Core authorizes the organization/user/project request and forwards trusted identity and
   classification claims to Nova using service authentication; client-supplied identity
   or classification fields are ignored.
3. Nova loads the current policy, evaluates admission, reserves budget/rate capacity, and
   creates the initial audit record before retrieval or provider access.
4. Relay/Core return only permission-filtered context. Nova checks context-source,
   provider, geography, contract, and storage obligations before constructing a prompt.
5. Nova invokes the resolved approved model and validates structured output. Each retry
   repeats relevant policy, budget, and rate checks.
6. Any tool call returns through a registered, schema-validated, service-authenticated
   Core/Relay endpoint. The owning service performs independent resource authorization.
7. Mutations remain proposals. Core validates exact version, authenticated approver,
   current authorization, current policy, target optimistic-lock versions, and approval
   binding immediately before committing business state.
8. Nova and Core finalize linked audit events and reconcile usage/cost. Append failure
   events if finalization is delayed; repair jobs must be idempotent and observable.

Policy caches MUST be versioned, bounded, and invalidated on activation/emergency disable.
If current policy cannot be established, provider calls, tools, and mutations fail closed.

## 8. Governance acceptance criteria

### AC-1: Frontend bypass resistance

Given a disabled capability, disallowed model, unauthorized context source, or forbidden
tool, direct calls to Core/Nova internal paths with manipulated client fields are denied
by server-side authorization and policy checks. The audit record contains the denial and
no provider call, tool side effect, or mutation occurs.

### AC-2: Runtime capability disablement

An authorized administrator can disable a capability through the governance control
without rebuilding or redeploying any service. Subsequent requests are denied immediately;
an in-flight run cannot begin another provider call or mutation after the disable takes
effect. Disable and recovery actions appear in policy/incident history.

### AC-3: Approval enforcement for high-impact mutations

Every tool classified `MUTATE_HIGH_IMPACT` or `EXTERNAL_COMMUNICATION` is blocked until
the configured valid approval exists. Tests cover missing, self-approved when prohibited,
expired, rejected, stale-version, changed-payload, changed-policy, and revoked-permission
approvals. None can commit business state.

### AC-4: Safe audit export

Compliance-export tests seed credentials, tokens, hidden prompts, provider error bodies,
personal data, and retained prompt content. Default and metadata-only exports contain none
of those prohibited values and include documented omission/redaction reasons. Explicit
content export succeeds only with policy permission, field permission, approved purpose,
and an audited access event.

### AC-5: Complete usage and cost attribution

For successful, denied, failed, retried, fallback, and tool-mediated runs, dashboard and
ledger reconciliation can aggregate request count, tokens, and estimated/confirmed cost
by organization, team, capability, provider, and resolved model. Missing provider usage
is visible as unknown or estimated, not silently recorded as zero.

### AC-6: Demonstrable retention

Automated tests create artifacts under each storage mode and retention class, advance past
expiry, execute the deletion workflow, and verify deletion from primary stores, indexes,
and caches subject to documented backup aging and legal holds. An evidence manifest proves
policy version, cutoff, counts, completion, failures/retries, legal-hold exclusions, and
checksums without containing deleted content.

### AC-7: Sensitive-project isolation

Restricted project material—including attachment text, retrieved snippets, embeddings,
tool results, cache entries, and retry payloads—never reaches an external provider when
external providers are excluded. Mixed-scope requests take the strictest classification
and either use an approved private route or fail with a visible limitation.

### AC-8: Geographic and contractual enforcement

Tests prove that expired contracts, disallowed processing/storage regions, unapproved
subprocessors, or incompatible retention/training terms deny routing and fallback. Audit
metadata identifies the policy reason and actual configured route without exposing secrets.

### AC-9: Audit completeness and integrity

Every admitted, denied, cancelled, failed, and completed run produces the required audit
fields, linked policy and proposal decisions, explicit unknown telemetry, and tamper
evidence. Restricted content access, export, policy changes, emergency actions, and repair
operations are also audited.

### AC-10: Transparent and safe user review

End-to-end tests confirm generated marking, safe organization-source disclosure, warnings,
incorrect-output reporting, immutable proposal/version review, configured approval, and a
safe support reference. The rendered UI and public API responses expose no hidden prompt,
secret, raw provider error, stack trace, or unauthorized source detail.

## 9. Operational evidence and review cadence

Organizations SHOULD review active capability/provider policy and contract expiry at
least quarterly, high-impact approval rules after relevant workflow changes, and budgets
monthly. TaskMind MUST alert before configured contract/review expiry and on deletion-job,
audit-finalization, or ledger-reconciliation failures.

The minimum recurring evidence set is: active policy and history, emergency-disable test,
provider/model/region inventory, denied-route tests, approval-control tests, role-access
review, usage/cost reconciliation, retention/deletion manifest, legal-hold exceptions,
and sampled sanitized audit exports. Evidence identifies the environment and policy
version so staging results cannot be mistaken for production assurance.

## 10. Out of scope and prohibited designs

- The frontend is not a policy enforcement point and does not call Nova or Relay directly.
- Governance does not grant models autonomous administrative access.
- “Model confidence” is not fabricated from model tone or presented as a probability
  unless the capability defines and validates that metric.
- Auditability does not justify retaining all prompts and responses.
- Provider fallback, retries, copied text, exports, embeddings, or support access do not
  create exceptions to data classification, geography, contract, or retention controls.
- Approval of one proposal does not authorize a different payload, version, target,
  recipient, later run, or future policy state.
