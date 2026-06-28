package com.taskmind.backend.tasktype.infrastructure.persistence.jpa;

import com.taskmind.backend.tasktype.domain.model.TaskTypeDefinition;
import jakarta.persistence.*;
import java.time.Instant;
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
    @Column(nullable = false) private boolean system;
    @Column(nullable = false) private boolean active;
    @Column(name = "sort_order") private Integer sortOrder;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected TaskTypeJpaEntity() {}

    private TaskTypeJpaEntity(TaskTypeDefinition d) {
        id = d.id(); version = d.version(); projectId = d.projectId(); key = d.key(); name = d.name(); color = d.color(); icon = d.icon(); system = d.system(); active = d.active(); sortOrder = d.sortOrder(); createdAt = d.createdAt(); updatedAt = d.updatedAt();
    }

    static TaskTypeJpaEntity fromDomain(TaskTypeDefinition d) { return new TaskTypeJpaEntity(d); }
    TaskTypeDefinition toDomain() { return new TaskTypeDefinition(id, version, projectId, key, name, color, icon, system, active, sortOrder, createdAt, updatedAt); }
}
