package com.taskmind.backend.project.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.taskmind.backend.project.application.health.ProjectHealthApplicationService;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskLink;
import com.taskmind.backend.task.domain.model.TaskLinkType;
import com.taskmind.backend.task.domain.model.TaskSource;
import com.taskmind.backend.task.domain.model.TaskStatus;
import com.taskmind.backend.task.domain.repository.TaskLinkRepository;
import com.taskmind.backend.task.domain.repository.TaskRepository;
import com.taskmind.backend.task.infrastructure.persistence.jpa.TaskReleaseStatsProjection;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectHealthApplicationServiceTest {

    private final UUID projectId = UUID.randomUUID();
    private final UUID assigneeId = UUID.randomUUID();
    private final InMemoryTaskRepository tasks = new InMemoryTaskRepository();
    private final InMemoryTaskLinkRepository links = new InMemoryTaskLinkRepository();
    private final ProjectHealthApplicationService service = new ProjectHealthApplicationService(tasks, links);

    @Test
    void returnsEmptyHealthForProjectWithoutTasks() {
        var health = service.calculate(projectId);

        assertEquals(0, health.totalTaskCount());
        assertEquals(0, health.completionPercentage());
        assertEquals(0, health.overdueTaskCount());
        assertEquals(0, health.workloadByAssignee().size());
    }

    @Test
    void calculatesProjectHealthMetrics() {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var blocked = task(TaskStatus.TODO, assigneeId, now.plusDays(2), Instant.now().minusSeconds(60));
        tasks.items.add(task(TaskStatus.DONE, assigneeId, now.minusDays(1), Instant.now()));
        tasks.items.add(task(TaskStatus.TODO, null, now.minusDays(1), Instant.now().minusSeconds(16L * 24 * 60 * 60)));
        tasks.items.add(blocked);
        links.items.add(new TaskLink(UUID.randomUUID(), null, UUID.randomUUID(), blocked.id(), TaskLinkType.BLOCKS, assigneeId, Instant.now()));

        var health = service.calculate(projectId);

        assertEquals(3, health.totalTaskCount());
        assertEquals(1, health.completedTaskCount());
        assertEquals(33, health.completionPercentage());
        assertEquals(1, health.overdueTaskCount());
        assertEquals(1, health.blockedTaskCount());
        assertEquals(1, health.unassignedTaskCount());
        assertEquals(1, health.staleTaskCount());
        assertEquals(1, health.upcomingDeadlineRiskCount());
        assertEquals(1, health.workloadByAssignee().get(0).activeTaskCount());
    }

    private Task task(TaskStatus status, UUID assignee, OffsetDateTime dueAt, Instant updatedAt) {
        return new Task(UUID.randomUUID(), null, UUID.randomUUID(), projectId, null, assignee, null, null, (String) null, null, null, null, "Task", null, status, 2, dueAt, null, null, TaskSource.MANUAL, BigDecimal.ONE, Instant.now(), updatedAt);
    }

    static class InMemoryTaskRepository implements TaskRepository {
        final List<Task> items = new ArrayList<>();
        public Task save(Task task) { return task; }
        public Optional<Task> findById(UUID id) { return Optional.empty(); }
        public Optional<Task> findByIdForUpdate(UUID id) { return Optional.empty(); }
        public List<Task> findAll() { return items; }
        public List<Task> findChildren(UUID parentId) { return List.of(); }
        public List<Task> findAncestors(UUID id) { return List.of(); }
        public List<TaskReleaseStatsProjection> releaseStats(UUID projectId) { return List.of(); }
        public List<Task> findFiltered(Optional<UUID> userId, Optional<TaskStatus> status, boolean overdueOnly, OffsetDateTime now, int page, int size) { return List.of(); }
    }

    static class InMemoryTaskLinkRepository implements TaskLinkRepository {
        final List<TaskLink> items = new ArrayList<>();
        public TaskLink save(TaskLink link) { return link; }
        public List<TaskLink> findForTask(UUID taskId) { return items.stream().filter(link -> taskId.equals(link.sourceTaskId()) || taskId.equals(link.targetTaskId())).toList(); }
        public void deleteById(UUID id) {}
        public Optional<TaskLink> findById(UUID id) { return Optional.empty(); }
    }
}
