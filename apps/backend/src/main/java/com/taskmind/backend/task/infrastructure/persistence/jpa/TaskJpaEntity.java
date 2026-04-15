package com.taskmind.backend.task.infrastructure.persistence.jpa;

import com.taskmind.backend.task.domain.model.EnergyLevel;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskSource;
import com.taskmind.backend.task.domain.model.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "project_id")
    private UUID projectId;

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

    protected TaskJpaEntity() {
    }

    private TaskJpaEntity(Task task) {
        this.id = task.id();
        this.userId = task.userId();
        this.projectId = task.projectId();
        this.title = task.title();
        this.description = task.description();
        this.status = task.status();
        this.priority = task.priority();
        this.dueAt = task.dueAt();
        this.durationMinutes = task.durationMinutes();
        this.energyLevel = task.energyLevel();
        this.source = task.source();
        this.confidence = task.confidence();
        this.createdAt = task.createdAt();
        this.updatedAt = task.updatedAt();
    }

    public static TaskJpaEntity fromDomain(Task task) {
        return new TaskJpaEntity(task);
    }

    public Task toDomain() {
        return new Task(
            id,
            userId,
            projectId,
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
            updatedAt
        );
    }
}
