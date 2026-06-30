package com.taskmind.backend.tasktype.infrastructure.persistence.jpa;

import com.taskmind.backend.task.domain.model.TaskLevel;
import com.taskmind.backend.tasktype.domain.model.TaskTypeDefinition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "task_types")
public class TaskTypeJpaEntity {
    @Id private UUID id;
    @Version @Column(nullable = false) private Long version;
    @Column(name = "project_id") private UUID projectId;
    @Column(name = "type_key", nullable = false) private String key;
    @Column(nullable = false) private String name;
    private String color;
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_task_level", nullable = false)
    private TaskLevel defaultTaskLevel;

    @Column(name = "allowed_task_levels", nullable = false)
    private String allowedTaskLevels;

    @Column(name = "is_container", nullable = false)
    private boolean container;

    @Column(name = "allow_children", nullable = false)
    private boolean allowChildren;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_kind")
    private TaskTypeDefinition.SystemKind systemKind;

    @Column(nullable = false) private boolean system;
    @Column(nullable = false) private boolean active;
    @Column(name = "sort_order") private Integer sortOrder;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected TaskTypeJpaEntity() {}

    private TaskTypeJpaEntity(TaskTypeDefinition d) {
        id = d.id();
        version = d.version();
        projectId = d.projectId();
        key = d.key();
        name = d.name();
        color = d.color();
        icon = d.icon();
        defaultTaskLevel = d.defaultTaskLevel();
        allowedTaskLevels = encodeLevels(d.allowedTaskLevels());
        container = d.container();
        allowChildren = d.allowChildren();
        systemKind = d.systemKind();
        system = d.system();
        active = d.active();
        sortOrder = d.sortOrder();
        createdAt = d.createdAt();
        updatedAt = d.updatedAt();
    }

    static TaskTypeJpaEntity fromDomain(TaskTypeDefinition d) {
        return new TaskTypeJpaEntity(d);
    }

    TaskTypeDefinition toDomain() {
        return new TaskTypeDefinition(
                id,
                version,
                projectId,
                key,
                name,
                color,
                icon,
                defaultTaskLevel,
                decodeLevels(allowedTaskLevels, defaultTaskLevel),
                container,
                allowChildren,
                systemKind,
                system,
                active,
                sortOrder,
                createdAt,
                updatedAt);
    }

    private static String encodeLevels(Set<TaskLevel> levels) {
        return String.join(",", levels.stream().map(Enum::name).sorted().toList());
    }

    private static Set<TaskLevel> decodeLevels(String value, TaskLevel fallback) {
        if (value == null || value.isBlank()) return Set.of(fallback);
        EnumSet<TaskLevel> levels = EnumSet.noneOf(TaskLevel.class);
        for (String part : value.split(",")) levels.add(TaskLevel.valueOf(part.trim()));
        return levels;
    }
}
