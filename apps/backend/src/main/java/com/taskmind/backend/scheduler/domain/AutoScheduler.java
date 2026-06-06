package com.taskmind.backend.scheduler.domain;

import com.taskmind.backend.scheduler.domain.model.ScheduledBlock;
import com.taskmind.backend.scheduler.domain.model.ScheduledBlockStatus;
import com.taskmind.backend.scheduler.domain.model.SchedulingPreferences;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
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
        return schedule(userId, preferences, tasks, List.of(), from, to);
    }

    public List<ScheduledBlock> schedule(
            UUID userId,
            SchedulingPreferences preferences,
            List<Task> tasks,
            List<ScheduledBlock> occupiedBlocks,
            OffsetDateTime from,
            OffsetDateTime to) {
        Instant now = Instant.now();
        List<Task> sorted =
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
        ArrayList<ScheduledBlock> occupied = new ArrayList<>(occupiedBlocks);
        OffsetDateTime cursor = nextWindowStart(from, preferences);
        ArrayList<ScheduledBlock> blocks = new ArrayList<ScheduledBlock>();
        for (Task task : sorted) {
            int minutes =
                    roundedDuration(
                            task.durationMinutes() != null
                                    ? task.durationMinutes()
                                    : estimateMinutes(task),
                            preferences.blockGranularityMinutes());
            OffsetDateTime taskCursor = cursor;
            int dailyMinutes = minutesAlreadyBooked(occupied, taskCursor);
            LocalDate day = taskCursor.toLocalDate();
            while (!taskCursor.plusMinutes(minutes).isAfter(to)) {
                if (!taskCursor.toLocalDate().equals(day)) {
                    day = taskCursor.toLocalDate();
                    dailyMinutes = minutesAlreadyBooked(occupied, taskCursor);
                }
                OffsetDateTime dayEnd = taskCursor.with(preferences.workdayEnd());
                OffsetDateTime end = taskCursor.plusMinutes(minutes);
                ScheduledBlock overlapping = firstOverlap(occupied, taskCursor, end);
                if (overlapping != null) {
                    taskCursor = nextWindowStart(overlapping.endsAt(), preferences);
                    continue;
                }
                if (dailyMinutes + minutes <= preferences.maxDailyFocusMinutes()
                        && !end.isAfter(dayEnd)) {
                    ScheduledBlock block =
                            new ScheduledBlock(
                                    UUID.randomUUID(),
                                    null,
                                    userId,
                                    task.id(),
                                    taskCursor,
                                    end,
                                    ScheduledBlockStatus.SCHEDULED,
                                    "Auto-scheduled by priority, due date, and availability",
                                    null,
                                    null,
                                    now,
                                    now);
                    blocks.add(block);
                    occupied.add(block);
                    cursor = end;
                    break;
                }
                taskCursor =
                        nextWindowStart(
                                taskCursor.plusDays(1).with(preferences.workdayStart()),
                                preferences);
            }
        }
        return blocks;
    }

    private OffsetDateTime nextWindowStart(
            OffsetDateTime candidate, SchedulingPreferences preferences) {
        OffsetDateTime start = candidate.with(preferences.workdayStart());
        OffsetDateTime end = candidate.with(preferences.workdayEnd());
        if (candidate.isBefore(start)) return start;
        if (!candidate.isBefore(end)) return candidate.plusDays(1).with(preferences.workdayStart());
        return candidate;
    }

    private ScheduledBlock firstOverlap(
            List<ScheduledBlock> blocks,
            OffsetDateTime candidateStart,
            OffsetDateTime candidateEnd) {
        return blocks.stream()
                .filter(block -> block.status() == ScheduledBlockStatus.SCHEDULED)
                .filter(
                        block ->
                                candidateStart.isBefore(block.endsAt())
                                        && candidateEnd.isAfter(block.startsAt()))
                .min(Comparator.comparing(ScheduledBlock::startsAt))
                .orElse(null);
    }

    private int minutesAlreadyBooked(List<ScheduledBlock> blocks, OffsetDateTime cursor) {
        return blocks.stream()
                .filter(block -> block.status() == ScheduledBlockStatus.SCHEDULED)
                .filter(block -> block.startsAt().toLocalDate().equals(cursor.toLocalDate()))
                .mapToInt(
                        block ->
                                (int)
                                        java.time.Duration.between(block.startsAt(), block.endsAt())
                                                .toMinutes())
                .sum();
    }

    private int estimateMinutes(Task task) {
        return Math.max(30, (task.storyPoints() == null ? 1 : task.storyPoints()) * 30);
    }

    private int roundedDuration(int minutes, int granularity) {
        return (int) (Math.ceil((double) minutes / granularity) * granularity);
    }
}
