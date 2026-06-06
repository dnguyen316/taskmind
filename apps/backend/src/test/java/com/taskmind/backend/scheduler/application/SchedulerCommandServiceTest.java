package com.taskmind.backend.scheduler.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.scheduler.domain.AutoScheduler;
import com.taskmind.backend.scheduler.domain.RescheduleProposalEngine;
import com.taskmind.backend.scheduler.domain.model.ScheduledBlock;
import com.taskmind.backend.scheduler.domain.model.ScheduledBlockStatus;
import com.taskmind.backend.scheduler.domain.model.SchedulingPreferences;
import com.taskmind.backend.scheduler.domain.repository.ScheduledBlockRepository;
import com.taskmind.backend.scheduler.domain.repository.SchedulingPreferencesRepository;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskLevel;
import com.taskmind.backend.task.domain.model.TaskSource;
import com.taskmind.backend.task.domain.model.TaskStatus;
import com.taskmind.backend.task.domain.model.TaskType;
import com.taskmind.backend.task.domain.repository.TaskRepository;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class SchedulerCommandServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-05T12:00:00Z");

    @Test
    void createsDefaultPreferencesForUser() {
        var preferences = new InMemoryPreferences();
        var service = new SchedulerCommandService(preferences, new InMemoryBlocks(), new InMemoryTasks(), new AutoScheduler());
        var user = new AuthenticatedUser(UUID.randomUUID(), Set.of("USER"));

        var created = service.preferencesFor(user);

        assertEquals(user.userId(), created.userId());
        assertEquals(LocalTime.of(9, 0), created.workdayStart());
        assertEquals(LocalTime.of(17, 0), created.workdayEnd());
        assertEquals(30, created.blockGranularityMinutes());
        assertEquals(360, created.maxDailyFocusMinutes());
        assertEquals(created, preferences.findByUserId(user.userId()).orElseThrow());
    }

    @Test
    void updatesPreferencesWhenVersionMatchesAndRejectsStaleVersions() {
        var user = new AuthenticatedUser(UUID.randomUUID(), Set.of("USER"));
        var preferences = new InMemoryPreferences();
        preferences.save(
                new SchedulingPreferences(
                        UUID.randomUUID(),
                        7L,
                        user.userId(),
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0),
                        30,
                        360,
                        NOW,
                        NOW));
        var service = new SchedulerCommandService(preferences, new InMemoryBlocks(), new InMemoryTasks(), new AutoScheduler());

        var updated = service.updatePreferences(user, 7L, LocalTime.of(8, 30), LocalTime.of(15, 30), 30, 240);

        assertEquals(LocalTime.of(8, 30), updated.workdayStart());
        assertEquals(240, updated.maxDailyFocusMinutes());
        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> service.updatePreferences(user, 6L, LocalTime.of(9, 0), LocalTime.of(17, 0), 30, 360));
    }

    @Test
    void persistsGeneratedScheduleAndAvoidsDuplicateTaskBlocks() {
        var user = new AuthenticatedUser(UUID.randomUUID(), Set.of("USER"));
        var blocks = new InMemoryBlocks();
        var tasks = new InMemoryTasks();
        var task = task(user.userId(), "schedule me", TaskStatus.IN_PROGRESS, 30, 1, NOW);
        tasks.save(task);
        var service = new SchedulerCommandService(new InMemoryPreferences(), blocks, tasks, new AutoScheduler());
        var command =
                new GenerateScheduleCommand(
                        OffsetDateTime.parse("2026-06-08T08:00:00Z"),
                        OffsetDateTime.parse("2026-06-09T18:00:00Z"));

        var generated = service.generate(user, command);
        var generatedAgain = service.generate(user, command);

        assertEquals(1, generated.size());
        assertEquals(task.id(), generated.get(0).taskId());
        assertEquals(OffsetDateTime.parse("2026-06-08T09:00:00Z"), generated.get(0).startsAt());
        assertTrue(generatedAgain.isEmpty());
        assertEquals(1, blocks.findByUserIdBetween(user.userId(), command.from(), command.to()).size());
    }

    @Test
    void generatesRescheduleProposalsForMissedBlocks() {
        var user = new AuthenticatedUser(UUID.randomUUID(), Set.of("USER"));
        var blocks = new InMemoryBlocks();
        var overdue =
                block(
                        user.userId(),
                        UUID.randomUUID(),
                        OffsetDateTime.now().minusDays(2),
                        OffsetDateTime.now().minusDays(2).plusHours(1),
                        ScheduledBlockStatus.SCHEDULED);
        blocks.save(overdue);
        var commands = new SchedulerCommandService(new InMemoryPreferences(), blocks, new InMemoryTasks(), new AutoScheduler());
        var proposals = new SchedulerProposalService(blocks, new RescheduleProposalEngine(), commands);

        var result = proposals.overdueProposals(user);

        assertEquals(1, result.size());
        assertEquals(overdue.id(), result.get(0).blockId());
        assertEquals(overdue.taskId(), result.get(0).taskId());
        assertEquals("Block is overdue and should be moved", result.get(0).reason());
        assertEquals(ScheduledBlockStatus.MISSED, blocks.findById(overdue.id()).orElseThrow().status());
    }

    @Test
    void rejectsInvalidGenerationWindow() {
        var service = new SchedulerCommandService(new InMemoryPreferences(), new InMemoryBlocks(), new InMemoryTasks(), new AutoScheduler());
        var user = new AuthenticatedUser(UUID.randomUUID(), Set.of("USER"));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.generate(
                                user,
                                new GenerateScheduleCommand(
                                        OffsetDateTime.parse("2026-06-06T09:00:00Z"),
                                        OffsetDateTime.parse("2026-06-05T09:00:00Z"))));
    }

    private static ScheduledBlock block(
            UUID userId,
            UUID taskId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            ScheduledBlockStatus status) {
        return new ScheduledBlock(
                UUID.randomUUID(), null, userId, taskId, startsAt, endsAt, status, "test", null, null, NOW, NOW);
    }

    private static Task task(
            UUID userId, String title, TaskStatus status, Integer duration, int priority, Instant createdAt) {
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
                status,
                priority,
                OffsetDateTime.parse("2026-06-10T12:00:00Z"),
                duration,
                null,
                TaskSource.MANUAL,
                null,
                createdAt,
                createdAt);
    }

    private static final class InMemoryPreferences implements SchedulingPreferencesRepository {
        private final Map<UUID, SchedulingPreferences> preferences = new LinkedHashMap<>();

        public SchedulingPreferences save(SchedulingPreferences preferences) {
            this.preferences.put(preferences.userId(), preferences);
            return preferences;
        }

        public Optional<SchedulingPreferences> findByUserId(UUID userId) {
            return Optional.ofNullable(preferences.get(userId));
        }

        public Optional<SchedulingPreferences> findByUserIdForUpdate(UUID userId) {
            return findByUserId(userId);
        }
    }

    private static final class InMemoryBlocks implements ScheduledBlockRepository {
        private final Map<UUID, ScheduledBlock> blocks = new LinkedHashMap<>();

        public ScheduledBlock save(ScheduledBlock block) {
            blocks.put(block.id(), block);
            return block;
        }

        public List<ScheduledBlock> saveAll(List<ScheduledBlock> blocks) {
            blocks.forEach(this::save);
            return blocks;
        }

        public Optional<ScheduledBlock> findById(UUID id) {
            return Optional.ofNullable(blocks.get(id));
        }

        public Optional<ScheduledBlock> findByIdForUpdate(UUID id) {
            return findById(id);
        }

        public List<ScheduledBlock> findByUserIdBetween(UUID userId, OffsetDateTime from, OffsetDateTime to) {
            return blocks.values().stream()
                    .filter(block -> block.userId().equals(userId))
                    .filter(block -> block.startsAt().isBefore(to) && block.endsAt().isAfter(from))
                    .toList();
        }
    }

    private static final class InMemoryTasks implements TaskRepository {
        private final List<Task> tasks = new ArrayList<>();

        public Task save(Task task) {
            tasks.add(task);
            return task;
        }

        public Optional<Task> findById(UUID id) {
            return tasks.stream().filter(task -> task.id().equals(id)).findFirst();
        }

        public Optional<Task> findByIdForUpdate(UUID id) {
            return findById(id);
        }

        public List<Task> findAll() {
            return List.copyOf(tasks);
        }

        public List<Task> findChildren(UUID parentId) {
            return List.of();
        }

        public List<Task> findAncestors(UUID id) {
            return List.of();
        }

        public List<com.taskmind.backend.task.infrastructure.persistence.jpa.TaskReleaseStatsProjection> releaseStats(
                UUID projectId) {
            return List.of();
        }

        public List<Task> findFiltered(
                Optional<UUID> userId,
                Optional<TaskStatus> status,
                boolean overdueOnly,
                OffsetDateTime now,
                int page,
                int size) {
            return tasks.stream()
                    .filter(task -> userId.map(id -> task.userId().equals(id)).orElse(true))
                    .filter(task -> status.map(value -> task.status() == value).orElse(true))
                    .toList();
        }
    }
}
