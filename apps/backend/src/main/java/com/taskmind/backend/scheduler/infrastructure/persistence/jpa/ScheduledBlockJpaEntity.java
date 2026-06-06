package com.taskmind.backend.scheduler.infrastructure.persistence.jpa;

import com.taskmind.backend.scheduler.domain.model.ScheduledBlock;
import com.taskmind.backend.scheduler.domain.model.ScheduledBlockStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduled_blocks")
public class ScheduledBlockJpaEntity {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduledBlockStatus status;

    @Column(length = 500)
    private String rationale;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ScheduledBlockJpaEntity() {}

    private ScheduledBlockJpaEntity(ScheduledBlock block) {
        id = block.id();
        version = block.version();
        userId = block.userId();
        taskId = block.taskId();
        startsAt = block.startsAt();
        endsAt = block.endsAt();
        status = block.status();
        rationale = block.rationale();
        completedAt = block.completedAt();
        deletedAt = block.deletedAt();
        createdAt = block.createdAt();
        updatedAt = block.updatedAt();
    }

    public static ScheduledBlockJpaEntity fromDomain(ScheduledBlock block) {
        return new ScheduledBlockJpaEntity(block);
    }

    public ScheduledBlock toDomain() {
        return new ScheduledBlock(
                id,
                version,
                userId,
                taskId,
                startsAt,
                endsAt,
                status,
                rationale,
                completedAt,
                deletedAt,
                createdAt,
                updatedAt);
    }
}
