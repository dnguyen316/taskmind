package com.taskmind.backend.scheduler.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
    @Test
    void placesTasksInsidePreferenceWindowByDueDate() {
        var userId = UUID.randomUUID();
        var now = Instant.parse("2026-06-05T12:00:00Z");
        var preferences =
                new SchedulingPreferences(
                        UUID.randomUUID(),
                        null,
                        userId,
                        LocalTime.of(9, 0),
                        LocalTime.of(12, 0),
                        30,
                        180,
                        now,
                        now);
        var later = task(userId, "later", OffsetDateTime.parse("2026-06-08T12:00:00Z"), 90, now);
        var sooner = task(userId, "sooner", OffsetDateTime.parse("2026-06-06T12:00:00Z"), 60, now);

        var blocks =
                new AutoScheduler()
                        .schedule(
                                userId,
                                preferences,
                                List.of(later, sooner),
                                OffsetDateTime.parse("2026-06-05T08:00:00Z"),
                                OffsetDateTime.parse("2026-06-07T18:00:00Z"));

        assertEquals(2, blocks.size());
        assertEquals(sooner.id(), blocks.get(0).taskId());
        assertEquals(LocalTime.of(9, 0), blocks.get(0).startsAt().toLocalTime());
        assertFalse(blocks.get(0).endsAt().toLocalTime().isAfter(LocalTime.of(12, 0)));
    }

    private Task task(UUID userId, String title, OffsetDateTime dueAt, int duration, Instant now) {
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
                null,
                null,
                null,
                title,
                null,
                TaskStatus.TODO,
                2,
                dueAt,
                duration,
                null,
                TaskSource.MANUAL,
                null,
                now,
                now);
    }
}
