package com.taskmind.backend.scheduler.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.scheduler.domain.AutoScheduler;
import com.taskmind.backend.scheduler.domain.model.ScheduledBlock;
import com.taskmind.backend.scheduler.domain.model.SchedulingPreferences;
import com.taskmind.backend.scheduler.domain.repository.ScheduledBlockRepository;
import com.taskmind.backend.scheduler.domain.repository.SchedulingPreferencesRepository;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskStatus;
import com.taskmind.backend.task.domain.repository.TaskRepository;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SchedulerCommandServiceTest {
    @Test
    void createsDefaultPreferencesForUser() {
        var service =
                new SchedulerCommandService(
                        new InMemoryPreferences(),
                        new InMemoryBlocks(),
                        new EmptyTasks(),
                        new AutoScheduler());
        var preferences =
                service.preferencesFor(new AuthenticatedUser(UUID.randomUUID(), Set.of("USER")));
        assertEquals(LocalTime.of(9, 0), preferences.workdayStart());
        assertEquals(30, preferences.blockGranularityMinutes());
    }

    @Test
    void rejectsInvalidGenerationWindow() {
        var service =
                new SchedulerCommandService(
                        new InMemoryPreferences(),
                        new InMemoryBlocks(),
                        new EmptyTasks(),
                        new AutoScheduler());
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

    private static final class InMemoryPreferences implements SchedulingPreferencesRepository {
        private SchedulingPreferences preferences;

        public SchedulingPreferences save(SchedulingPreferences preferences) {
            this.preferences = preferences;
            return preferences;
        }

        public Optional<SchedulingPreferences> findByUserId(UUID userId) {
            return Optional.ofNullable(preferences)
                    .filter(existing -> existing.userId().equals(userId));
        }

        public Optional<SchedulingPreferences> findByUserIdForUpdate(UUID userId) {
            return findByUserId(userId);
        }
    }

    private static final class InMemoryBlocks implements ScheduledBlockRepository {
        private final List<ScheduledBlock> blocks = new ArrayList<>();

        public ScheduledBlock save(ScheduledBlock block) {
            blocks.add(block);
            return block;
        }

        public List<ScheduledBlock> saveAll(List<ScheduledBlock> blocks) {
            this.blocks.addAll(blocks);
            return blocks;
        }

        public Optional<ScheduledBlock> findById(UUID id) {
            return blocks.stream().filter(block -> block.id().equals(id)).findFirst();
        }

        public Optional<ScheduledBlock> findByIdForUpdate(UUID id) {
            return findById(id);
        }

        public List<ScheduledBlock> findByUserIdBetween(
                UUID userId, OffsetDateTime from, OffsetDateTime to) {
            return blocks.stream().filter(block -> block.userId().equals(userId)).toList();
        }
    }

    private static final class EmptyTasks implements TaskRepository {
        public Task save(Task task) {
            return task;
        }

        public Optional<Task> findById(UUID id) {
            return Optional.empty();
        }

        public Optional<Task> findByIdForUpdate(UUID id) {
            return Optional.empty();
        }

        public List<Task> findAll() {
            return List.of();
        }

        public List<Task> findChildren(UUID parentId) {
            return List.of();
        }

        public List<Task> findAncestors(UUID id) {
            return List.of();
        }

        public List<
                        com.taskmind.backend.task.infrastructure.persistence.jpa
                                .TaskReleaseStatsProjection>
                releaseStats(UUID projectId) {
            return List.of();
        }

        public List<Task> findFiltered(
                Optional<UUID> userId,
                Optional<TaskStatus> status,
                boolean overdueOnly,
                OffsetDateTime now,
                int page,
                int size) {
            return List.of();
        }
    }
}
