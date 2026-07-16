package com.taskmind.backend.specbreakdown.infrastructure.persistence.jpa;

import com.taskmind.backend.specbreakdown.application.SpecBreakdownProcessingJob;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownJobStatus;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownJobType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spec_breakdown_jobs")
public class SpecBreakdownJobJpaEntity {
    @Id
    UUID id;

    @Version
    Long version;

    UUID draftId;
    UUID userId;

    @Enumerated(EnumType.STRING)
    SpecBreakdownJobType aiJobType;

    @Enumerated(EnumType.STRING)
    SpecBreakdownJobStatus status;

    @Column(columnDefinition = "TEXT")
    String checkpoint;

    UUID novaRunId;

    @Column(columnDefinition = "TEXT")
    String errorMessage;

    boolean requestedCancel;
    boolean paused;
    Instant createdAt;
    Instant updatedAt;
    Instant completedAt;

    protected SpecBreakdownJobJpaEntity() {}

    static SpecBreakdownJobJpaEntity from(SpecBreakdownProcessingJob job) {
        SpecBreakdownJobJpaEntity entity = new SpecBreakdownJobJpaEntity();
        entity.id = job.id();
        entity.version = job.version();
        entity.draftId = job.draftId();
        entity.userId = job.userId();
        entity.aiJobType = job.aiJobType();
        entity.status = job.status();
        entity.checkpoint = job.checkpoint();
        entity.novaRunId = job.novaRunId();
        entity.errorMessage = job.errorMessage();
        entity.requestedCancel = job.requestedCancel();
        entity.paused = job.paused();
        entity.createdAt = job.createdAt();
        entity.updatedAt = job.updatedAt();
        entity.completedAt = job.completedAt();
        return entity;
    }

    SpecBreakdownProcessingJob toDomain() {
        return new SpecBreakdownProcessingJob(
                id,
                version,
                draftId,
                userId,
                aiJobType,
                status,
                checkpoint,
                novaRunId,
                errorMessage,
                requestedCancel,
                paused,
                createdAt,
                updatedAt,
                completedAt);
    }
}
