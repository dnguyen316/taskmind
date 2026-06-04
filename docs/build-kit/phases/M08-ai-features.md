# M08 - AI Features (end to end)

## Objective

Deliver the user-facing AI capabilities end to end: capture, goal breakdown, weekly review,
project brief, task describe/autocomplete/translate, scheduler duration-estimate/rationale,
and dashboard insights. Each flow is **FE -> Core facade -> Nova capability -> typed result**,
with deterministic local fallbacks.

## Depends on

[M07](M07-nova-ai.md) (Nova + facades), [M05](M05-eventing-relay.md) (Relay context for
review/insights), [M04](M04-scheduler.md) (scheduler hooks), and
[M03](M03-frontend-shell.md).

## Scope

**In:** Core `ai` facade application service and the `/v1/ai/**` plus planner/review
endpoints, local fallbacks, AI-event publishing, wiring the M07 capabilities into real
flows, frontend `ai` feature (capture, goal breakdown, weekly review,
describe/translate widgets, Nova chat widget app-wide), and scheduler AI assists
(duration estimate and rationale).

**Out:** Spec breakdown pipeline (M09).

## Files to create

```text
# Core AI facade
apps/backend/src/main/java/.../ai/application/AiFacadeApplicationService.java
apps/backend/src/main/java/.../ai/application/AiFacadeLocalFallbacks.java
apps/backend/src/main/java/.../ai/application/CaptureResult.java
apps/backend/src/main/java/.../ai/application/DescribeTaskResult.java
apps/backend/src/main/java/.../ai/application/DescribeTaskAutocompleteResult.java
apps/backend/src/main/java/.../ai/application/TranslateTaskResult.java
apps/backend/src/main/java/.../ai/application/AiDomainEventPublisher.java

# AI BFF endpoints live on owning feature modules
apps/backend/src/main/java/.../task/interfaces/rest/PlanningController.java
  # /v1/ai/capture, /goals/{id}/breakdown,
  # /tasks/describe/autocomplete, /tasks/translate,
  # /planner/**, /review/weekly/generate
apps/backend/src/main/java/.../project/interfaces/rest/ProjectBriefController.java
  # /v1/projects/{id}/ai-brief

# Scheduler AI assists
apps/backend/src/main/java/.../scheduler/.../*.java
  # duration-estimate + rationale wiring to Nova

# Nova capability implementations and tests
apps/ai/src/main/java/.../capability/.../*.java
  # capture, goal_breakdown, weekly_review, project_brief,
  # describe_task, autocomplete_task, translate_task,
  # duration_estimate, rationale_phrase, dashboard_insights

# Frontend ai feature
apps/frontend/src/features/ai/pages/InboxCapturePage.vue
apps/frontend/src/features/ai/composables/useNovaChat.ts
apps/frontend/src/features/ai/composables/useCapture.ts
apps/frontend/src/features/ai/composables/useGoalBreakdown.ts
apps/frontend/src/features/ai/composables/useWeeklyReview.ts
apps/frontend/src/features/ai/composables/useDescribe.ts
apps/frontend/src/features/ai/composables/useTranslate.ts
apps/frontend/src/features/ai/components/*.vue
apps/frontend/src/components/AiAssistantWidget.vue
  # global chat widget, hidden on public routes
```

## Key design notes

- Each facade method calls `NovaAiClient` with the capability plus typed input from
  `libs/ai-contracts`, and falls back to `AiFacadeLocalFallbacks` (deterministic) if Nova
  is down so the UI never hard-fails.
- `weekly_review` and dashboard insights pull aggregated context from **Relay**
  (`RelayContextClient` inside Nova); Core just triggers the request.
- Emit `ai.*` domain events (`ai.capture_submitted`,
  `ai.suggestion_accepted`/`ai.suggestion_rejected`) via the outbox for the funnel
  projection.
- Keep the frontend talking only to Core; the Nova chat widget calls the Core facade at
  `/v1/nova/chat/stream`.
- For deterministic tests and local development, the default provider remains `mock`.
  Real providers (OpenAI/Anthropic/NAMC) are config-only. Document the slow-LLM SSE
  timeout note in the environment example.
- Keep all prompts, provider calls, capability routing, and `ai_runs` audit writes inside
  Nova. Core may orchestrate facades and persist accepted task/project changes, but it
  must not own LLM prompts.
- Keep `apps/backend/openapi.yaml` in sync for every Core request or response contract
  added by this milestone.

## Acceptance criteria

- [ ] Capture free text -> structured task drafts -> accept creates tasks.
- [ ] Goal breakdown produces milestones + sibling task drafts.
- [ ] Weekly review generates a narrative using Relay context.
- [ ] Project brief generates from project state.
- [ ] Task describe / autocomplete / translate work inline in the editor.
- [ ] Scheduler shows AI duration estimate + rationale on blocks.
- [ ] Nova chat widget answers (mock) and can chain capture.
- [ ] Local fallbacks return sensible output when Nova is unavailable.

## Verification

```bash
cd apps/backend && mvn -q -Dtest='*AiFacade*,*Planning*,*Describe*,*Translate*' test
cd apps/ai && mvn -q -Dtest='*Capture*,*Goal*,*WeeklyReview*,*Translate*,*Autocomplete*' test
make vibe-verify
# Browser E2E: capture -> accept; goal breakdown; weekly review; chat reply
```

## Definition of Done

All AI features work FE -> Core -> Nova with deterministic fallbacks; `ai.*` events flow;
tests and `make vibe-verify` are green; browser E2E confirms capture and chat.
