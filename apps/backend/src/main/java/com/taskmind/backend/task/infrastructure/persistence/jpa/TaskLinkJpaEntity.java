package com.taskmind.backend.task.infrastructure.persistence.jpa;

import com.taskmind.backend.task.domain.model.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_links")
public class TaskLinkJpaEntity {
    @Id private UUID id;
    @Version private Long version;

    @Column(name = "source_task_id")
    private UUID sourceTaskId;

    @Column(name = "target_task_id")
    private UUID targetTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type")
    private TaskLinkType linkType;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at")
    private Instant createdAt;

    protected TaskLinkJpaEntity() {}

    private TaskLinkJpaEntity(TaskLink link) {
        id = link.id();
        version = link.version();
        sourceTaskId = link.sourceTaskId();
        targetTaskId = link.targetTaskId();
        linkType = link.linkType();
        createdByUserId = link.createdByUserId();
        createdAt = link.createdAt();
    }

    public static TaskLinkJpaEntity fromDomain(TaskLink link) {
        return new TaskLinkJpaEntity(link);
    }

    public TaskLink toDomain() {
        return new TaskLink(
                id, version, sourceTaskId, targetTaskId, linkType, createdByUserId, createdAt);
    }
}
