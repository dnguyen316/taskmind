package com.taskmind.backend.scheduler.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.scheduler.domain.AutoScheduler;
import com.taskmind.backend.scheduler.domain.model.ScheduledBlock;
import com.taskmind.backend.scheduler.domain.model.ScheduledBlockStatus;
import com.taskmind.backend.scheduler.domain.model.SchedulingPreferences;
import com.taskmind.backend.scheduler.domain.repository.ScheduledBlockRepository;
import com.taskmind.backend.scheduler.domain.repository.SchedulingPreferencesRepository;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskStatus;
import com.taskmind.backend.task.domain.repository.TaskRepository;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SchedulerCommandService {
    private final SchedulingPreferencesRepository preferences;
    private final ScheduledBlockRepository blocks;
    private final TaskRepository tasks;
    private final AutoScheduler autoScheduler;

    public SchedulerCommandService(
            SchedulingPreferencesRepository preferences,
            ScheduledBlockRepository blocks,
            TaskRepository tasks,
            AutoScheduler autoScheduler) {
        this.preferences = preferences;
        this.blocks = blocks;
        this.tasks = tasks;
        this.autoScheduler = autoScheduler;
    }

    @Transactional
    public SchedulingPreferences preferencesFor(AuthenticatedUser requester) {
        return preferences
                .findByUserId(requester.userId())
                .orElseGet(
                        () ->
                                preferences.save(
                                        SchedulingPreferences.defaultsFor(
                                                requester.userId(), Instant.now())));
    }

    @Transactional
    public SchedulingPreferences updatePreferences(
            AuthenticatedUser requester,
            Long version,
            LocalTime start,
            LocalTime end,
            int granularity,
            int maxFocus) {
        SchedulingPreferences existing =
                preferences
                        .findByUserIdForUpdate(requester.userId())
                        .orElseGet(
                                () ->
                                        SchedulingPreferences.defaultsFor(
                                                requester.userId(), Instant.now()));
        if (version != null && existing.version() != null && !version.equals(existing.version())) {
            throw new ObjectOptimisticLockingFailureException(
                    SchedulingPreferences.class, existing.id());
        }
        return preferences.save(existing.updated(start, end, granularity, maxFocus, Instant.now()));
    }

    @Transactional
    public List<ScheduledBlock> generate(
            AuthenticatedUser requester, GenerateScheduleCommand command) {
        SchedulingPreferences prefs = preferencesFor(requester);
        OffsetDateTime from = command.from() == null ? OffsetDateTime.now() : command.from();
        OffsetDateTime to = command.to() == null ? from.plusDays(7) : command.to();
        if (!from.isBefore(to))
            throw new IllegalArgumentException("Schedule generation window is invalid");
        markMissedBlocks(requester, OffsetDateTime.now());
        List<ScheduledBlock> occupiedBlocks =
                blocks.findByUserIdBetween(requester.userId(), from, to);
        Set<UUID> alreadyScheduledTaskIds =
                occupiedBlocks.stream()
                        .filter(
                                block ->
                                        block.status() == ScheduledBlockStatus.SCHEDULED
                                                || block.status() == ScheduledBlockStatus.COMPLETED)
                        .map(ScheduledBlock::taskId)
                        .collect(Collectors.toSet());
        List<Task> schedulableTasks =
                schedulableTasks(requester).stream()
                        .filter(task -> !alreadyScheduledTaskIds.contains(task.id()))
                        .toList();
        return blocks.saveAll(
                autoScheduler.schedule(
                        requester.userId(), prefs, schedulableTasks, occupiedBlocks, from, to));
    }

    @Transactional
    public List<ScheduledBlock> listBlocks(
            AuthenticatedUser requester, OffsetDateTime from, OffsetDateTime to) {
        markMissedBlocks(requester, OffsetDateTime.now());
        OffsetDateTime windowStart = from == null ? OffsetDateTime.now().minusDays(7) : from;
        OffsetDateTime windowEnd = to == null ? OffsetDateTime.now().plusDays(30) : to;
        return blocks.findByUserIdBetween(requester.userId(), windowStart, windowEnd);
    }

    @Transactional
    public Optional<ScheduledBlock> updateBlock(
            AuthenticatedUser requester,
            UUID id,
            Long version,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String rationale) {
        return blocks.findByIdForUpdate(id)
                .map(
                        existing -> {
                            validateOwner(requester, existing);
                            if (version != null && !version.equals(existing.version()))
                                throw new ObjectOptimisticLockingFailureException(
                                        ScheduledBlock.class, id);
                            ScheduledBlock updated =
                                    existing.rescheduled(
                                            startsAt != null ? startsAt : existing.startsAt(),
                                            endsAt != null ? endsAt : existing.endsAt(),
                                            rationale != null ? rationale : existing.rationale(),
                                            Instant.now());
                            return blocks.save(updated);
                        });
    }

    @Transactional
    public Optional<ScheduledBlock> completeBlock(AuthenticatedUser requester, UUID id) {
        return blocks.findByIdForUpdate(id)
                .map(
                        existing -> {
                            validateOwner(requester, existing);
                            return blocks.save(
                                    existing.completed(OffsetDateTime.now(), Instant.now()));
                        });
    }

    @Transactional
    public List<ScheduledBlock> markMissedBlocks(AuthenticatedUser requester, OffsetDateTime now) {
        List<ScheduledBlock> missed =
                blocks.findByUserIdBetween(requester.userId(), now.minusDays(30), now).stream()
                        .filter(block -> block.shouldMarkMissed(now))
                        .map(block -> block.missed(Instant.now()))
                        .toList();
        return missed.isEmpty() ? List.of() : blocks.saveAll(missed);
    }

    private List<Task> schedulableTasks(AuthenticatedUser requester) {
        OffsetDateTime now = OffsetDateTime.now();
        List<Task> todo =
                tasks.findFiltered(
                        Optional.of(requester.userId()),
                        Optional.of(TaskStatus.TODO),
                        false,
                        now,
                        0,
                        100);
        List<Task> inProgress =
                tasks.findFiltered(
                        Optional.of(requester.userId()),
                        Optional.of(TaskStatus.IN_PROGRESS),
                        false,
                        now,
                        0,
                        100);
        return Stream.concat(todo.stream(), inProgress.stream())
                .collect(
                        Collectors.toMap(
                                Task::id,
                                task -> task,
                                (left, right) -> left,
                                java.util.LinkedHashMap::new))
                .values()
                .stream()
                .toList();
    }

    private void validateOwner(AuthenticatedUser requester, ScheduledBlock block) {
        if (!requester.isPrivileged() && !requester.userId().equals(block.userId()))
            throw new IllegalArgumentException("Cannot modify another user's scheduled block");
    }
}
