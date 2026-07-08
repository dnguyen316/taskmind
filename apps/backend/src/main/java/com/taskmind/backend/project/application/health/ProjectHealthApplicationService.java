package com.taskmind.backend.project.application.health;

import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskLinkType;
import com.taskmind.backend.task.domain.model.TaskStatus;
import com.taskmind.backend.task.domain.repository.TaskLinkRepository;
import com.taskmind.backend.task.domain.repository.TaskRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProjectHealthApplicationService {

    private static final int STALE_DAYS = 14;
    private static final int UPCOMING_RISK_DAYS = 7;

    private final TaskRepository taskRepository;
    private final TaskLinkRepository taskLinkRepository;

    public ProjectHealthApplicationService(TaskRepository taskRepository, TaskLinkRepository taskLinkRepository) {
        this.taskRepository = taskRepository;
        this.taskLinkRepository = taskLinkRepository;
    }

    public ProjectHealthResponse calculate(UUID projectId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Instant staleBefore = now.minusDays(STALE_DAYS).toInstant();
        OffsetDateTime riskWindowEnd = now.plusDays(UPCOMING_RISK_DAYS);

        java.util.List<Task> tasks = taskRepository.findAll().stream()
                .filter(task -> projectId.equals(task.projectId()))
                .filter(task -> task.deletedAt() == null)
                .filter(task -> task.status() != TaskStatus.ARCHIVED)
                .toList();

        int total = tasks.size();
        int completed = count(tasks.stream().filter(task -> task.status() == TaskStatus.DONE).count());
        int active = count(tasks.stream().filter(ProjectHealthApplicationService::isActive).count());
        int completionPercentage = total == 0 ? 0 : (int) Math.round((completed * 100.0) / total);
        int overdue = count(tasks.stream().filter(task -> isActive(task) && task.dueAt() != null && task.dueAt().isBefore(now)).count());
        int blocked = count(tasks.stream().filter(task -> isActive(task) && isBlocked(task)).count());
        int unassigned = count(tasks.stream().filter(task -> isActive(task) && task.assigneeId() == null).count());
        int stale = count(tasks.stream().filter(task -> isActive(task) && task.updatedAt().isBefore(staleBefore)).count());
        int upcomingRisk = count(tasks.stream()
                .filter(task -> isActive(task) && task.dueAt() != null)
                .filter(task -> !task.dueAt().isBefore(now) && task.dueAt().isBefore(riskWindowEnd))
                .count());

        java.util.List<ProjectHealthResponse.AssigneeWorkload> workload = tasks.stream()
                .filter(ProjectHealthApplicationService::isActive)
                .filter(task -> task.assigneeId() != null)
                .collect(Collectors.groupingBy(Task::assigneeId, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new ProjectHealthResponse.AssigneeWorkload(entry.getKey(), count(entry.getValue())))
                .toList();

        return new ProjectHealthResponse(
                projectId,
                total,
                completed,
                completionPercentage,
                overdue,
                blocked,
                unassigned,
                stale,
                upcomingRisk,
                workload,
                narrative(active, overdue, blocked, upcomingRisk),
                Instant.now());
    }

    private static boolean isActive(Task task) {
        return task.status() != TaskStatus.DONE && task.status() != TaskStatus.ARCHIVED;
    }

    private boolean isBlocked(Task task) {
        return taskLinkRepository.findForTask(task.id()).stream()
                .anyMatch(link -> task.id().equals(link.targetTaskId()) && link.linkType() == TaskLinkType.BLOCKS);
    }

    private static String narrative(int active, int overdue, int blocked, int upcomingRisk) {
        if (active == 0) {
            return "No active work is currently at risk.";
        }
        if (overdue > 0 || blocked > 0) {
            return "Project health needs attention: resolve overdue and blocked work before adding new scope.";
        }
        if (upcomingRisk > 0) {
            return "Project health is watch-listed because upcoming deadlines may need focus.";
        }
        return "Project health is stable based on current deterministic task metrics.";
    }

    private static int count(long value) {
        return Math.toIntExact(value);
    }
}
