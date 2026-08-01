# Meeting-to-Execution Product Requirements Document

| Metadata | Value                                                                                                      |
| -------- | ---------------------------------------------------------------------------------------------------------- |
| Status   | Proposed for first release and pilot validation                                                            |
| Owner    | Product                                                                                                    |
| Audience | Product, design, engineering, security, and pilot stakeholders                                             |
| Scope    | Product requirements and architectural constraints; this document does not define implementation contracts |

## 1. Purpose

Meeting-to-Execution turns completed meeting notes or transcripts into a reviewable,
evidence-linked proposal for decisions, actions, risks, dependencies, and follow-up. It
reduces post-meeting administration without allowing AI output to silently become
business state.

The first release must optimize for trustworthy human review rather than autonomous
execution. TaskMind proposes structured outcomes, exposes uncertainty and source
evidence, and applies only the items an authorized user explicitly accepts.

## 2. Goals and non-goals

### Goals

- Convert meeting artifacts into a structured proposal in one workflow.
- Help reviewers resolve ownership, dates, project context, and duplicate work quickly.
- Support item-by-item correction and partial acceptance before any records change.
- Preserve provenance from every accepted record to the source meeting and passage.
- Notify owners once, with a consolidated view of newly assigned or updated work.
- Make proposal generation and application safe to retry without duplicate records.

### First-release non-goals

- Live meeting recording, transcription, or participation by a meeting bot.
- Automatic calendar ingestion, calendar-triggered processing, or attendee import.
- Autonomous acceptance, assignment, merging, task closure, or decision creation.
- Treating inferred details as if they were explicitly stated in the source.
- Replacing TaskMind's existing authorization or project access model.

Live meeting recording and automatic calendar ingestion are explicitly deferred to a
later release. Their future designs must not be assumed by first-release data or API
contracts.

## 3. Users and jobs to be done

### Primary user

A meeting organizer, project manager, team lead, or authorized participant who needs to
turn a completed meeting into accountable work while retaining final control.

### Secondary users

- Proposed action owners who need a clear, consolidated notification of commitments.
- Project stakeholders who need traceable decisions, risks, blockers, and follow-up.
- Auditors or administrators who need to understand the source, proposal, reviewer
  changes, authorization decisions, and resulting records.

### Core job

> After a meeting, help me identify and validate what was decided and what must happen
> next, then update TaskMind only with the outcomes I approve.

## 4. First-release inputs

At least one source input is required:

1. **Pasted meeting notes.** Plain text or rich text entered directly into the workflow.
2. **Uploaded transcript file.** A supported text-bearing file uploaded as the meeting's
   source artifact. The product must validate file type and size, report extraction
   failures clearly, and retain the original artifact according to the meeting's access
   and retention policy.

The user may also provide:

- **Project:** an optional TaskMind project used to constrain context and duplicate
  detection.
- **Meeting date:** an optional date used to interpret relative dates in the source.
- **Attendee list:** optional TaskMind users and/or literal attendee names supplied by the
  user. A literal name does not by itself authorize assignment or establish a TaskMind
  identity.

If both pasted notes and a file are present, the review must distinguish their passages
and preserve citations to the correct source. The first release does not capture audio or
video and does not fetch calendar events.

## 5. Structured proposal output

The generated proposal must contain the following sections, including an explicit empty
state when no supported item is found:

### 5.1 Meeting summary

A concise account of the meeting's purpose, material discussion, and outcomes. The
summary remains proposed content until the review is completed and must link its material
claims to source passages.

### 5.2 Decisions

Each proposed decision includes:

- decision statement;
- rationale;
- proposed decision owner;
- project, when supported or selected; and
- one or more links to supporting source passages.

### 5.3 Action items

Each proposed action includes:

- task title and optional description;
- proposed owner;
- proposed due date;
- proposed priority;
- proposed project;
- relationship to other proposed actions and relevant existing tasks;
- duplicate-handling recommendation; and
- one or more links to supporting source passages.

### 5.4 Risks and blockers

Each proposed risk or blocker includes a description, affected project or action when
known, proposed owner or escalation target when explicitly supported, and links to the
source evidence.

### 5.5 Dependencies

The proposal identifies dependencies:

- between proposed actions;
- from a proposed action to an existing TaskMind task; and
- from an existing task to a proposed action when the proposed work blocks it.

Every proposed existing-task mapping includes field-level confidence and evidence for
the match. A dependency suggestion does not alter either task until confirmed.

### 5.6 Unresolved questions

Questions that remain unanswered or ambiguous are listed separately rather than being
converted into invented decisions, dates, owners, or commitments. Each question links to
the passage that raised it or records that it arose from a clearly labeled inference.

### 5.7 Follow-up and approvals

The proposal identifies any explicitly requested follow-up meeting or approval, including
the proposed participants or approver only when supported by the source or user-supplied
context. It does not schedule a meeting or grant approval.

### 5.8 Source links and provenance

Every proposed item must link to one or more stable source passages. A passage link opens
the relevant excerpt and identifies its source artifact and location, such as paragraph,
line range, page, or transcript segment. Accepted records retain a durable reference to
the meeting, proposal item, and cited passages even if the presentation layer later
changes.

## 6. End-to-end user journey

1. **Provide source.** The user pastes notes or uploads a transcript, and may select a
   project, meeting date, and attendees. TaskMind validates the source and permissions.
2. **Detect context.** TaskMind detects likely project and participants using only context
   the user may access. Detected identities remain suggestions and show their evidence
   and confidence.
3. **Generate proposal.** AI extracts structured outcomes and returns a proposal. It does
   not create or update tasks, decisions, meetings, risks, assignments, or notifications.
4. **Review by outcome.** The review screen groups the summary, decisions, tasks, risks and
   blockers, dependencies, unresolved questions, and follow-up or approval requirements.
   The reviewer can inspect source evidence and confidence without leaving the review.
5. **Resolve each suggestion.** The user may edit, accept, reject, or merge individual
   suggestions. Bulk actions may speed review but must preserve item-level visibility and
   honor all confirmation and authorization requirements. The user may complete a review
   with only some items accepted.
6. **Apply accepted outcomes.** TaskMind creates or updates the appropriate records only
   for accepted items and records the accepted values, reviewer, source provenance, and
   application result. Unauthorized operations remain unapplied with a clear explanation.
7. **Notify owners.** After successful application, each affected owner receives one
   consolidated notification covering their accepted assignments or material updates,
   with links to the records and source meeting. Reprocessing or retrying the same
   acceptance does not resend equivalent notifications.
8. **Complete and audit.** The meeting moves to a completion state that distinguishes a
   fully reviewed proposal from one with unapplied failures. Its audit trail records
   generation, regeneration, edits, item-level decisions, merges, applications,
   authorization outcomes, notifications, and retries.

## 7. Review and meeting states

The user experience must distinguish at least these lifecycle states:

- **Draft:** source or optional context is still being entered.
- **Processing:** source extraction or proposal generation is in progress.
- **Ready for review:** a versioned proposal is available and has not been completed.
- **Partially reviewed:** at least one item has a saved review decision while other items
  remain unresolved.
- **Applying:** accepted outcomes are being applied to Core business state.
- **Completed:** all proposal items have a terminal review disposition and all accepted
  items were either applied successfully or explicitly resolved by the reviewer.
- **Needs attention:** source processing, generation, authorization, or application left
  an actionable failure. Successfully applied items remain applied and visible.

Regenerating a proposal creates a new proposal version; it must not erase the review or
audit history of earlier versions. Completion does not imply that every suggestion was
accepted.

## 8. Confidence and grounding behavior

### Field-level confidence

For every proposed action, the review displays field-level confidence for:

- owner;
- due date;
- project; and
- mapping to an existing task.

Confidence must be represented consistently, with an accessible label and explanation,
not by color alone. The product team must agree on and configure the threshold before the
pilot. Any field below that threshold requires explicit manual confirmation or correction
before its value can be applied. A review-level score must not replace these field-level
checks.

### Grounding rules

- Never assign work based solely on a name collision. Identity matching must use stronger
  evidence such as an explicitly supplied attendee identity, unambiguous TaskMind mention,
  project membership plus corroborating source context, or manual reviewer selection.
- Display **“not found in source”** next to any proposed value derived from context or
  inference rather than stated in the meeting artifact.
- Do not invent attendees, commitments, or deadlines. When the source does not support a
  value, leave it unset, mark it for confirmation, or represent the ambiguity as an
  unresolved question.
- User-supplied project, date, or attendee context must remain distinguishable from
  source-stated evidence.
- Relative dates may be normalized only when a meeting date is known; the normalized date
  remains reviewable and retains the original phrase as evidence.
- An inferred priority may be proposed only when labeled **“not found in source”** and
  cannot bypass review.

## 9. Duplicate and existing-task handling

For each proposed action, TaskMind compares the proposal with open tasks in the selected
project. If no project is selected or confidently confirmed, the product must not search
projects the user cannot access and must make the reduced duplicate-check scope clear.

The review recommends exactly one disposition where possible:

- **Link:** preserve separate work and add a confirmed relationship to an existing task.
- **Merge:** create no separate task and combine confirmed new detail into the selected
  existing task.
- **Update:** apply confirmed field changes to the selected existing task without treating
  the proposal as a separate task.
- **Create:** create a new task when no suitable open task is identified.

Each recommendation shows:

- the candidate task's title, status, owner, due date, and project as permitted;
- similarity or mapping confidence;
- source passages supporting the proposed action;
- existing-task fields that support the match; and
- a plain-language reason for the recommended disposition.

The user can choose a different disposition or task. TaskMind never merges, updates,
closes, or otherwise mutates an existing task without explicit confirmation. Duplicate
detection must not recommend a closed task as the default merge or update target.

## 10. Permissions, privacy, and AI-provider policy

- Only users who can view a project may select it or use its tasks, participants,
  activity, or other context for detection, generation, or duplicate comparison.
- Only users authorized under existing TaskMind rules may assign another person or create
  a decision. Displaying an AI suggestion does not grant the reviewer that permission.
- Permissions are rechecked when accepted outcomes are applied, not only when the review
  is opened, so stale access cannot authorize a write.
- Source artifacts and their extracted passages follow the same access policy as the
  meeting. Source links must not expose content to a user who cannot view that meeting.
- Audit views obey the same authorization constraints and must not leak restricted source
  text or project context.
- Sensitive projects must be configurable as excluded from external AI providers. When a
  selected or detected project has that policy, TaskMind must use an allowed processing
  path or stop with a clear policy message; it must not silently send the source or
  project context externally.
- Context retrieval, prompts, logs, telemetry, and error reporting must minimize personal
  and sensitive data and honor configured retention and provider-use policies.

## 11. Reliability, idempotency, and audit requirements

### Idempotency

Applying accepted items uses a stable identity for the meeting, proposal version, and
proposal item. Retrying the same accepted proposal item returns or reconciles the original
application result rather than creating a second task, decision, dependency, audit event,
or equivalent notification. A deliberate later edit and re-acceptance must be represented
as a new revision, not confused with a transport retry.

### Partial failure

Items apply independently so that one failure does not roll back unrelated successful
items. The review shows applied, rejected, pending, and failed items separately and lets
the reviewer retry only unresolved applications. Any merge or update must use TaskMind's
normal concurrency protection and surface conflicts for renewed confirmation.

### Audit trail

The audit trail must record, at minimum:

- meeting and source artifact identifiers;
- proposal and prompt/model version metadata;
- proposal item and cited passage identifiers;
- generated values, confidence, and inference labels;
- reviewer edits and item-level accept, reject, merge, update, link, or create choices;
- acting user, timestamps, authorization result, and resulting record identifiers;
- application attempt identity and outcome; and
- consolidated notification outcome.

Audit data must support traceability without making hidden AI reasoning a product
requirement.

## 12. First-release acceptance criteria

The first release is acceptable when all of the following are demonstrated:

1. A user can upload a supported transcript, generate a structured proposal, and
   selectively accept its action items.
2. Every accepted item remains traceable to its source meeting, proposal item, and source
   passage or passages.
3. Proposed owners and dates below the agreed field-confidence threshold cannot be
   applied until the reviewer confirms or corrects them.
4. Duplicate recommendations show their evidence and never silently update, merge, or
   close an existing task.
5. A reviewer can accept some suggestions and reject or leave out others, then complete
   the workflow without accepting the entire proposal.
6. Retrying application of the same accepted proposal does not create duplicate business
   records or equivalent notifications.
7. An ambiguous name collision cannot result in an assignment without corroborating
   identity evidence or manual selection.
8. Inferred fields are visibly labeled **“not found in source,”** and unsupported
   attendees, commitments, and deadlines are absent rather than fabricated.
9. Unauthorized project context is neither retrieved nor displayed, and permissions are
   enforced again when outcomes are applied.
10. A sensitive project's source and context are not sent to a disallowed external AI
    provider.

## 13. Pilot success targets

Measure the pilot at the completed-meeting level and report medians and distributions,
not only aggregate totals.

| Target                                                     | Definition                                                                                                                                                                                            |
| ---------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Median review time below five minutes                      | Median elapsed active review time from first opening the generated proposal to completion, excluding background processing and idle time according to the agreed analytics timeout                    |
| At least 70% of proposed actions accepted                  | Accepted or edited-and-accepted action suggestions divided by action suggestions given a terminal review disposition; report unedited and edited acceptance separately                                |
| Fewer than 5% of accepted actions deleted as incorrect     | Accepted actions deleted as incorrect within the agreed observation window divided by accepted actions; report merges, ordinary completion, and scope cancellation separately from incorrect deletion |
| At least 50% reduction in post-meeting administrative time | Median reduction versus a documented pre-pilot baseline for comparable meetings, using the same start/end boundary and participant role                                                               |
| At least 80% owner completion                              | Accepted actions with a confirmed owner before review completion divided by all accepted actions                                                                                                      |

Pilot reporting should also capture proposal-generation failures, low-confidence field
rates, duplicate recommendation outcomes, reviewer edits, permission failures,
idempotency replays, notification delivery, and the share of details labeled “not found
in source.” These diagnostics explain target movement but do not replace the targets.

## 14. Architectural constraints for future implementation

This PRD intentionally does not prescribe endpoints, schemas, events, provider selection,
or component design. Any future implementation must preserve these boundaries:

- **Core owns accepted business state.** It owns meeting/source authorization,
  application of accepted outcomes, idempotency, resulting TaskMind records, and the
  externally exposed facade.
- **Nova owns extraction and proposal generation.** Prompts, model/provider calls,
  grounding and structured proposal generation remain in Nova; Nova does not directly
  write accepted task, project, decision, or assignment state.
- **Relay provides searchable context.** Relay supplies permission-scoped searchable and
  projected context, including candidates used for retrieval; it does not write business
  state.
- **The frontend communicates only with Core.** It never calls Nova or Relay directly.

These are architectural constraints only. Detailed service contracts belong in the
appropriate build-kit milestone and reference documents when implementation is planned.

## 15. Open product decisions before implementation

The following choices require explicit agreement before development begins:

- supported transcript file formats, encodings, and maximum size;
- the pilot's field-confidence scale, threshold, and calibration process;
- rules for bulk acceptance when one or more fields require confirmation;
- the observation window and reason taxonomy for “deleted as incorrect”;
- the active-review idle timeout used in the five-minute metric;
- retention and deletion behavior for source files, extracted passages, proposals, and
  audit metadata;
- the allowed processing path when external providers are excluded; and
- notification channels and the consolidation window.
