package com.taskmind.backend.scheduler.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.taskmind.backend.scheduler.domain.model.ScheduledBlock;
import com.taskmind.backend.scheduler.domain.model.ScheduledBlockStatus;
import com.taskmind.backend.scheduler.domain.model.SchedulingPreferences;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskLevel;
import com.taskmind.backend.task.domain.model.TaskSource;
import com.taskmind.backend.task.domain.model.TaskStatus;
import com.taskmind.backend.task.domain.model.TaskType;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AutoSchedulerTest {
    private static final Instant NOW = Instant.parse("2026-06-05T12:00:00Z");

    @Test
    void placesTasksInsidePreferenceWindows() {
        var userId = UUID.randomUUID();
        var preferences = preferences(userId, LocalTime.of(10, 0), LocalTime.of(12, 0), 30, 120);
        var task = task(userId, "inside window", null, 30, null, 1, NOW);

        var blocks =
                new AutoScheduler()
                        .schedule(
                                userId,
                                preferences,
                                List.of(task),
                                OffsetDateTime.parse("2026-06-05T08:00:00Z"),
                                OffsetDateTime.parse("2026-06-05T18:00:00Z"));

        assertEquals(1, blocks.size());
        assertEquals(LocalTime.of(10, 0), blocks.get(0).startsAt().toLocalTime());
        assertEquals(LocalTime.of(10, 30), blocks.get(0).endsAt().toLocalTime());
        assertFalse(blocks.get(0).endsAt().toLocalTime().isAfter(LocalTime.of(12, 0)));
    }

    @Test
    void ordersTasksByDueDateBeforePriorityAndCreationTime() {
        var userId = UUID.randomUUID();
        var preferences = preferences(userId, LocalTime.of(9, 0), LocalTime.of(12, 0), 30, 180);
        var laterDueHighPriority =
                task(userId, "later", OffsetDateTime.parse("2026-06-08T12:00:00Z"), 30, null, 0, NOW);
        var soonerDueLowPriority =
                task(userId, "sooner", OffsetDateTime.parse("2026-06-06T12:00:00Z"), 30, null, 9, NOW.plusSeconds(60));
        var noDue = task(userId, "no due", null, 30, null, 0, NOW.minusSeconds(60));

        var blocks =
                new AutoScheduler()
                        .schedule(
                                userId,
                                preferences,
                                List.of(noDue, laterDueHighPriority, soonerDueLowPriority),
                                OffsetDateTime.parse("2026-06-05T08:00:00Z"),
                                OffsetDateTime.parse("2026-06-05T18:00:00Z"));

        assertEquals(List.of(soonerDueLowPriority.id(), laterDueHighPriority.id(), noDue.id()),
                blocks.stream().map(ScheduledBlock::taskId).toList());
    }

    @Test
    void usesExplicitDurationBeforeStoryPointsAndRoundsToGranularity() {
        var userId = UUID.randomUUID();
        var preferences = preferences(userId, LocalTime.of(9, 0), LocalTime.of(17, 0), 30, 360);
        var explicitDuration = task(userId, "explicit", null, 45, 8, 1, NOW);
        var storyPointEstimate = task(userId, "estimate", null, null, 3, 2, NOW.plusSeconds(60));

        var blocks =
                new AutoScheduler()
                        .schedule(
                                userId,
                                preferences,
                                List.of(explicitDuration, storyPointEstimate),
                                OffsetDateTime.parse("2026-06-05T08:00:00Z"),
                                OffsetDateTime.parse("2026-06-05T18:00:00Z"));

        assertEquals(2, blocks.size());
        assertEquals(explicitDuration.id(), blocks.get(0).taskId());
        assertEquals(60, minutes(blocks.get(0)));
        assertEquals(storyPointEstimate.id(), blocks.get(1).taskId());
        assertEquals(90, minutes(blocks.get(1)));
    }

    @Test
    void skipsOccupiedBlocksWhenGeneratingMoreBlocks() {
        var userId = UUID.randomUUID();
        var preferences = preferences(userId, LocalTime.of(9, 0), LocalTime.of(12, 0), 30, 180);
        var occupiedTask = task(userId, "occupied", OffsetDateTime.parse("2026-06-06T12:00:00Z"), 60, null, 1, NOW);
        var newTask = task(userId, "new", OffsetDateTime.parse("2026-06-06T13:00:00Z"), 30, null, 1, NOW);
        var occupied = block(userId, occupiedTask.id(), "2026-06-05T09:00:00Z", "2026-06-05T10:00:00Z");

        var blocks =
                new AutoScheduler()
                        .schedule(
                                userId,
                                preferences,
                                List.of(newTask),
                                List.of(occupied),
                                OffsetDateTime.parse("2026-06-05T08:00:00Z"),
                                OffsetDateTime.parse("2026-06-05T18:00:00Z"));

        assertEquals(1, blocks.size());
        assertEquals(LocalTime.of(10, 0), blocks.get(0).startsAt().toLocalTime());
    }

    @Test
    void overflowTaskDoesNotPreventLaterTaskFromScheduling() {
        var userId = UUID.randomUUID();
        var preferences = preferences(userId, LocalTime.of(9, 0), LocalTime.of(11, 0), 30, 120);
        var tooLarge = task(userId, "too large", OffsetDateTime.parse("2026-06-05T10:00:00Z"), 180, null, 1, NOW);
        var fits = task(userId, "fits", OffsetDateTime.parse("2026-06-05T11:00:00Z"), 60, null, 1, NOW.plusSeconds(60));

        var blocks =
                new AutoScheduler()
                        .schedule(
                                userId,
                                preferences,
                                List.of(tooLarge, fits),
                                OffsetDateTime.parse("2026-06-05T08:00:00Z"),
                                OffsetDateTime.parse("2026-06-05T18:00:00Z"));

        assertEquals(1, blocks.size());
        assertEquals(fits.id(), blocks.get(0).taskId());
        assertEquals(LocalTime.of(9, 0), blocks.get(0).startsAt().toLocalTime());
    }

    @Test
    void returnsNoBlocksWhenNoSlotIsAvailable() {
        var userId = UUID.randomUUID();
        var preferences = preferences(userId, LocalTime.of(9, 0), LocalTime.of(10, 0), 30, 60);
        var task = task(userId, "blocked", null, 30, null, 1, NOW);
        var occupied = block(userId, UUID.randomUUID(), "2026-06-05T09:00:00Z", "2026-06-05T10:00:00Z");

        var blocks =
                new AutoScheduler()
                        .schedule(
                                userId,
                                preferences,
                                List.of(task),
                                List.of(occupied),
                                OffsetDateTime.parse("2026-06-05T08:00:00Z"),
                                OffsetDateTime.parse("2026-06-05T18:00:00Z"));

        assertTrue(blocks.isEmpty());
    }

    private SchedulingPreferences preferences(
            UUID userId, LocalTime start, LocalTime end, int granularity, int maxDailyFocus) {
        return new SchedulingPreferences(
                UUID.randomUUID(), null, userId, start, end, granularity, maxDailyFocus, NOW, NOW);
    }

    private ScheduledBlock block(UUID userId, UUID taskId, String startsAt, String endsAt) {
        return new ScheduledBlock(
                UUID.randomUUID(),
                null,
                userId,
                taskId,
                OffsetDateTime.parse(startsAt),
                OffsetDateTime.parse(endsAt),
                ScheduledBlockStatus.SCHEDULED,
                "Existing commitment",
                null,
                null,
                NOW,
                NOW);
    }

    private long minutes(ScheduledBlock block) {
        return java.time.Duration.between(block.startsAt(), block.endsAt()).toMinutes();
    }

    private Task task(
            UUID userId,
            String title,
            OffsetDateTime dueAt,
            Integer duration,
            Integer storyPoints,
            int priority,
            Instant createdAt) {
        return new Task(
                UUID.randomUUID(),
                null,
                userId,
                null,
                null,
                null,
                null,
                TaskLevel.TASK,
                TaskType.TASK,
                storyPoints,
                null,
                null,
                title,
                null,
                TaskStatus.TODO,
                priority,
                dueAt,
                duration,
                null,
                TaskSource.MANUAL,
                null,
                createdAt,
                createdAt);
    }
}
