# Frontend UI/UX Planning Context (Based on Backend Core Features Plan)

## 0) Current Execution Status (Updated April 16, 2026)

### Completed this cycle
- ✅ Frontend migration foundation from JavaScript to TypeScript started and applied to core app/runtime files (`main`, router, task API client, task composable, and task constants).
- ✅ Added shared task domain TypeScript models to enforce stronger request/response typing at the API boundary.
- ✅ Added TypeScript project configuration and `typecheck` script for ongoing static analysis in CI/local workflows.

### In progress
- 🔄 Extend TypeScript migration into Vue SFC `<script setup>` blocks for page/component-level prop/event typing.
- 🔄 Add runtime response validation adapters for AI/planner/review endpoints once those surfaces are implemented.

### Next planned tasks
1. Convert task components/pages to `lang="ts"` with explicit prop and emit interfaces.
2. Introduce typed store modules (`useTasksStore`, `usePlannerStore`, `useGoalsStore`, `useReviewStore`) to align with section 6.
3. Add schema-based runtime validation (e.g., Zod/Yup adapter layer) for external API responses.

---

## 1) Purpose
This document translates backend MVP capabilities into a practical frontend UI/UX plan so product/design/engineering can execute in parallel with clear API alignment.

Primary FE objective: provide clear, low-friction workflows that make AI suggestions understandable, editable, and trustworthy.

---

## 2) Product Principles for UI/UX
- **Clarity first**: show what AI did, why it did it, and what happens next.
- **Editable by default**: every generated artifact (task draft, plan, proposal) must be easy to adjust.
- **Safe actions**: destructive/bulk actions always require explicit confirmation.
- **Progressive disclosure**: keep default screens simple; reveal rationale/diagnostics on demand.
- **Fast feedback**: loading states, optimistic updates where safe, and background job status visibility.

---

## 3) Feature Mapping: Backend → Frontend Surfaces

### 3.1 AI Inbox Capture (`POST /v1/ai/capture`)
**Frontend surfaces**
- Inbox Capture panel (text + voice transcript paste)
- Parsed task draft list with confidence indicators
- Clarification prompt card when backend returns a question

**UI behaviors**
- Inline edit for title, due date, priority, and duration before save
- Confidence chip styles:
  - High (>=0.8): neutral/positive
  - Medium (0.5–0.79): warning-lite
  - Low (<0.5): needs review
- “Save all”, “Save selected”, and “Discard all” actions

**Guardrails in UI**
- No persistence until user confirms draft save
- Display source tag = `ai_capture` for transparency

### 3.2 Goal-to-Plan Breakdown (`POST /v1/ai/goals/{goalId}/breakdown`)
**Frontend surfaces**
- Goal detail page with “Generate plan” CTA
- Async generation state panel (queued, running, completed, failed)
- Draft Plan Editor (milestones + sequenced tasks + dependencies)

**UI behaviors**
- Render milestone timeline and task dependency badges
- Highlight “Immediate next action” block at top of draft
- Allow task reorder and lightweight dependency edits pre-save

**Guardrails in UI**
- Draft state is explicit and separate from committed tasks
- Show risk notes section without blocking edits

### 3.3 Smart Daily Planner (`POST /v1/planner/daily/generate`)
**Frontend surfaces**
- Daily Plan screen with inputs for available minutes and constraints
- Ordered plan list with rationale drawer per task
- Overflow section for deferred/not-fit tasks

**UI behaviors**
- One-click regenerate when constraints change
- Rationale drawer displays deterministic score factors (urgency, impact, effort, due-risk)
- Blocked tasks shown only when “Include blocked tasks” toggle is enabled

**Guardrails in UI**
- Clear annotation when a task is blocked or dependency-limited
- Preserve manual edits when regenerating (prompt before overwrite)

### 3.4 Adaptive Rescheduling (`POST /v1/planner/reschedule/proposals`)
**Frontend surfaces**
- Reschedule modal for selected/overdue tasks
- Proposal cards grouped by action (`move`, `split`, `defer`, `drop`)
- Conflict warnings panel (due date/dependency conflicts)

**UI behaviors**
- Multi-select proposals and preview resulting schedule changes
- Mandatory confirmation dialog before bulk apply
- “Why this proposal?” explainer snippet per card

**Guardrails in UI**
- Destructive choices (`drop`) require second confirmation
- Bulk apply requires confirmation token-aware flow (from backend)

### 3.5 Weekly Review Copilot (`POST /v1/review/weekly/generate`)
**Frontend surfaces**
- Weekly Review page with:
  - Summary block
  - Slippage insights list
  - Exactly 3 recommendations area
  - Next-week priority draft panel

**UI behaviors**
- Accept/edit/reject controls for each recommendation
- Track adoption state in UI telemetry events
- Export/share summary (phase 2)

**Guardrails in UI**
- If output shape is invalid, show “Regenerate review” fallback with explanatory message
- Keep recommendation count visually constrained to 3

---

## 4) Information Architecture

### Primary navigation (MVP)
1. **Inbox** (capture + draft triage)
2. **Tasks** (manual CRUD + filters)
3. **Planner** (daily plan + rescheduling)
4. **Goals** (goal planning + milestones)
5. **Review** (weekly copilot)

### Global UI elements
- Top-level “AI Activity” indicator (running jobs + latest outcomes)
- Toast system for async completion/failure
- User-safe trace id in error details for support/debug

---

## 5) UX States and Flows

### 5.1 Required states per AI-powered surface
- Idle
- Loading / generating
- Partial success (valid + needs review)
- Success
- Failure (recoverable)
- Failure (non-recoverable)

### 5.2 Empty-state content
- Explain value in one sentence
- Provide one primary CTA
- Include one example input (especially for capture and planner)

### 5.3 Confirmation patterns
- Bulk edits: modal + explicit copy of affected count
- Irreversible UX outcomes: second confirmation + warning style
- AI-generated overwrite paths: diff preview before commit

---

## 6) Component Plan (Vue 3)

### Shared components
- `AiConfidenceChip`
- `AiRationaleDrawer`
- `AsyncJobStatusPill`
- `ConfirmationModal`
- `TaskDraftEditorRow`
- `ConflictWarningList`
- `RecommendationCard`

### Page-level views
- `InboxCaptureView`
- `GoalPlanDraftView`
- `DailyPlannerView`
- `RescheduleProposalsModal`
- `WeeklyReviewView`

### Store/API modules
- `useTasksStore`
- `usePlannerStore`
- `useGoalsStore`
- `useReviewStore`
- `api/aiCapture.js`, `api/planner.js`, `api/goals.js`, `api/review.js`

---

## 7) API Contract Expectations (FE-facing)
- Strongly typed response adapters at boundary (runtime validation recommended).
- Include `confidence`, `rationale`, and `warnings` fields where applicable.
- Standard async job envelope for long-running operations:
  - `jobId`, `status`, `startedAt`, `completedAt`, `errorCode`, `traceId`
- Standard action audit metadata for mutation responses:
  - `source`, `promptVersion`, `model`, `decisionRequired`

---

## 8) Accessibility and Content UX
- Keyboard-first flows for all confirm/reject/edit actions
- ARIA labels for AI state changes and async status updates
- Color is never the only confidence indicator (add icon/text)
- Plain-language microcopy for uncertainty (e.g., “Needs your review”)

---

## 9) Analytics and Telemetry (Frontend)
Track events aligned with backend product metrics:
- `capture_submitted`, `capture_saved`, `capture_discarded`
- `plan_generated`, `plan_edited`, `plan_accepted`
- `reschedule_proposal_viewed`, `reschedule_applied`, `reschedule_rejected`
- `weekly_review_generated`, `recommendation_accepted|edited|rejected`

Each event should include stable ids (user/task/plan), timestamp, and correlation/trace id when available.

---

## 10) Delivery Plan for FE (Parallel to Backend)

### Phase A — Foundation (Weeks 1–2)
- Establish app shell, navigation, API client, and loading/error patterns
- Build shared components (status, confirmation, confidence chips)

### Phase B — Core AI Flows (Weeks 3–6)
- Inbox Capture flow end-to-end with editable drafts
- Daily Planner generation + rationale drawer
- Reschedule proposals modal + bulk confirmation

### Phase C — Goal + Review Workflows (Weeks 7–9)
- Goal breakdown draft editor with async states
- Weekly review page with recommendation decision controls

### Phase D — Quality/Hardening (Weeks 10–12)
- Accessibility pass, telemetry validation, edge-case UX states
- Contract hardening with backend schema checks

---

## 11) MVP Acceptance Criteria (Frontend)
- Users can capture, edit, and save AI-generated task drafts with confidence visibility.
- Users can generate a daily plan, understand rationale, and safely regenerate.
- Users can review and confirm rescheduling proposals with conflict context.
- Users can complete weekly review flow and decide on all recommendations.
- All AI mutation flows include explicit confirmation for bulk/destructive outcomes.

---

## 12) Open Questions for Product + Backend Sync
- What response schema should FE treat as canonical for rationale breakdown?
- Should FE poll async jobs or use server-sent events/websocket for status?
- Which fields are mandatory in failure responses (`errorCode`, `traceId`, `retryable`)?
- What is the exact contract for confirmation token acquisition/expiry?
