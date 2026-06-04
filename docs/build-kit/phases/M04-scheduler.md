# M04 - Scheduler

## Objective

Add time-aware scheduling: per-user scheduling preferences, scheduled calendar blocks, an
auto-scheduler that places tasks into blocks, and reschedule proposals. Surface a calendar
UI in the frontend. This is the Motion-style differentiator.

## Depends on

[M02](M02-tasks-projects.md) (tasks), [M03](M03-frontend-shell.md) (FE shell) for the UI.

## Scope

**In:** `scheduler` Core module (prefs, blocks, auto-schedule, reschedule proposals),
migrations V6/V9/V17, frontend `scheduler` feature (calendar page, prefs, Nova rail
placeholder).

**Out:** AI duration estimate / rationale phrase wiring (those call Nova; placeholders
here, real wiring in M08).

## Files to create

```text
# Migrations
V6__create_scheduler_tables.sql              # scheduling_preferences, scheduled_blocks
V9__replace_energy_with_story_points.sql     # if not already applied, trim scheduler energy windows
V17__add_scheduled_block_rationale.sql       # scheduled_blocks.rationale

# Scheduler module
scheduler/interfaces/rest/SchedulerController.java        # /v1/scheduler/{generate,blocks,preferences}
scheduler/interfaces/rest/dto/{...}.java
scheduler/application/{SchedulerCommandService,SchedulerProposalService,GenerateScheduleCommand}.java
scheduler/domain/model/{ScheduledBlock,SchedulingPreferences}.java
scheduler/domain/{AutoScheduler,RescheduleProposalEngine}.java
scheduler/domain/repository/{ScheduledBlockRepository,SchedulingPreferencesRepository}.java
scheduler/infrastructure/persistence/jpa/{...}.java

# Frontend
src/features/scheduler/pages/CalendarPage.vue
src/features/scheduler/components/{CalendarToolbar,NovaRail,SchedulingPreferences,...}.vue
src/features/scheduler/api/schedulerApi.ts
src/features/scheduler/composables/{...}.ts
```

## Key design notes

- `AutoScheduler` is pure domain logic (placement algorithm) - unit-test it thoroughly
  against preference windows, story points, and due dates.
- `RescheduleProposalEngine` proposes block moves when conflicts/overflow occur.
- `scheduled_blocks.rationale` holds a short human-readable reason (later AI-generated via
  the `rationale_phrase` capability in M08).
- Keep the Nova rail in the UI as a placeholder until M08 wires AI scheduling assists.
- See `docs/ai-auto-scheduler-prd.md` in the reference repo for the intended UX.

## Acceptance criteria

- [ ] Get/update scheduling preferences.
- [ ] `POST /v1/scheduler/generate` produces blocks respecting preferences + due dates.
- [ ] List/update/complete scheduled blocks; missed blocks handled.
- [ ] Reschedule proposals returned on conflict.
- [ ] Calendar UI renders blocks; preferences editable.

## Verification

```bash
mvn -q -Dtest='*Schedule*' test
make vibe-verify
# Browser E2E: open calendar, set preferences, generate schedule, see blocks
```

## Definition of Done

Scheduler module + calendar UI working; auto-scheduler unit-tested; `make vibe-verify`
green; browser E2E confirms generation.
