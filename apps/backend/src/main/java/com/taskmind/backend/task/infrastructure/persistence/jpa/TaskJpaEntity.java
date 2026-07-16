package com.taskmind.backend.task.infrastructure.persistence.jpa;

import com.taskmind.backend.task.domain.model.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class TaskJpaEntity {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "task_key")
    private String taskKey;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "parent_task_id")
    private UUID parentTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_level", nullable = false)
    private TaskLevel taskLevel;

    @Column(name = "task_type", nullable = false)
    private String taskType;

    @Column(name = "task_type_id")
    private UUID taskTypeId;

    @Column(name = "story_points")
    private Integer storyPoints;

    @Column(name = "release_version")
    private String releaseVersion;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(nullable = false)
    private int priority;

    @Column(name = "due_at")
    private OffsetDateTime dueAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "energy_level")
    private EnergyLevel energyLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskSource source;

    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskJpaEntity() {}

    private TaskJpaEntity(Task task) {
        id = task.id();
        version = task.version();
        userId = task.userId();
        projectId = task.projectId();
        taskKey = task.taskKey();
        assigneeId = task.assigneeId();
        parentTaskId = task.parentTaskId();
        taskLevel = task.taskLevel();
        taskType = task.taskType();
        storyPoints = task.storyPoints();
        releaseVersion = task.releaseVersion();
        deletedAt = task.deletedAt();
        title = task.title();
        description = task.description();
        status = task.status();
        priority = task.priority();
        dueAt = task.dueAt();
        durationMinutes = task.durationMinutes();
        energyLevel = task.energyLevel();
        source = task.source();
        confidence = task.confidence();
        createdAt = task.createdAt();
        updatedAt = task.updatedAt();
    }

    public static TaskJpaEntity fromDomain(Task task) {
        return new TaskJpaEntity(task);
    }

    public Task toDomain() {
        return new Task(
                id,
                version,
                userId,
                projectId,
                taskKey,
                assigneeId,
                parentTaskId,
                taskLevel,
                taskType,
                storyPoints,
                releaseVersion,
                deletedAt,
                title,
                description,
                status,
                priority,
                dueAt,
                durationMinutes,
                energyLevel,
                source,
                confidence,
                createdAt,
                updatedAt);
    }
}
