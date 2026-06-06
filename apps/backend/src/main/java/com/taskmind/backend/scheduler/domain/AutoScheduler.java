package com.taskmind.backend.scheduler.domain;

import com.taskmind.backend.scheduler.domain.model.ScheduledBlock;
import com.taskmind.backend.scheduler.domain.model.ScheduledBlockStatus;
import com.taskmind.backend.scheduler.domain.model.SchedulingPreferences;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AutoScheduler {
    public List<ScheduledBlock> schedule(
            UUID userId,
            SchedulingPreferences preferences,
            List<Task> tasks,
            OffsetDateTime from,
            OffsetDateTime to) {
        var now = Instant.now();
        var sorted =
                tasks.stream()
                        .filter(
                                task ->
                                        task.status() != TaskStatus.DONE
                                                && task.status() != TaskStatus.ARCHIVED)
                        .sorted(
                                Comparator.comparing(
                                                Task::dueAt,
                                                Comparator.nullsLast(Comparator.naturalOrder()))
                                        .thenComparing(Task::priority)
                                        .thenComparing(Task::createdAt))
                        .toList();
        var cursor = nextWindowStart(from, preferences);
        var blocks = new ArrayList<ScheduledBlock>();
        var dailyMinutes = 0;
        var day = cursor.toLocalDate();
        for (var task : sorted) {
            var minutes =
                    roundedDuration(
                            task.durationMinutes() != null
                                    ? task.durationMinutes()
                                    : estimateMinutes(task),
                            preferences.blockGranularityMinutes());
            while (!cursor.plusMinutes(minutes).isAfter(to)) {
                if (!cursor.toLocalDate().equals(day)) {
                    day = cursor.toLocalDate();
                    dailyMinutes = 0;
                }
                var dayEnd = cursor.with(preferences.workdayEnd());
                if (dailyMinutes + minutes <= preferences.maxDailyFocusMinutes()
                        && !cursor.plusMinutes(minutes).isAfter(dayEnd)) {
                    var end = cursor.plusMinutes(minutes);
                    blocks.add(
                            new ScheduledBlock(
                                    UUID.randomUUID(),
                                    null,
                                    userId,
                                    task.id(),
                                    cursor,
                                    end,
                                    ScheduledBlockStatus.SCHEDULED,
                                    "Auto-scheduled by priority and due date",
                                    null,
                                    null,
                                    now,
                                    now));
                    cursor = end;
                    dailyMinutes += minutes;
                    break;
                }
                cursor =
                        nextWindowStart(
                                cursor.plusDays(1).with(preferences.workdayStart()), preferences);
            }
        }
        return blocks;
    }

    private OffsetDateTime nextWindowStart(
            OffsetDateTime candidate, SchedulingPreferences preferences) {
        var start = candidate.with(preferences.workdayStart());
        var end = candidate.with(preferences.workdayEnd());
        if (candidate.isBefore(start)) return start;
        if (!candidate.isBefore(end)) return candidate.plusDays(1).with(preferences.workdayStart());
        return candidate;
    }

    private int estimateMinutes(Task task) {
        return Math.max(30, (task.storyPoints() == null ? 1 : task.storyPoints()) * 30);
    }

    private int roundedDuration(int minutes, int granularity) {
        return (int) (Math.ceil((double) minutes / granularity) * granularity);
    }
}
