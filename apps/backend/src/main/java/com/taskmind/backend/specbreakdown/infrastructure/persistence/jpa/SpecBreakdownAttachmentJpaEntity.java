package com.taskmind.backend.specbreakdown.infrastructure.persistence.jpa;

import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownAttachment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spec_breakdown_attachments")
public class SpecBreakdownAttachmentJpaEntity {
    @Id UUID id;
    UUID draftId;
    String fileName;
    String contentType;
    String storageKey;
    long sizeBytes;
    UUID createdByUserId;
    Instant createdAt;

    @Column(name = "deleted_at")
    Instant deletedAt;

    protected SpecBreakdownAttachmentJpaEntity() {}

    static SpecBreakdownAttachmentJpaEntity from(SpecBreakdownAttachment attachment) {
        SpecBreakdownAttachmentJpaEntity entity = new SpecBreakdownAttachmentJpaEntity();
        entity.id = attachment.id();
        entity.draftId = attachment.draftId();
        entity.fileName = attachment.fileName();
        entity.contentType = attachment.contentType();
        entity.storageKey = attachment.storageKey();
        entity.sizeBytes = attachment.sizeBytes();
        entity.createdByUserId = attachment.createdByUserId();
        entity.createdAt = attachment.createdAt();
        entity.deletedAt = attachment.deletedAt();
        return entity;
    }

    SpecBreakdownAttachment toDomain() {
        return new SpecBreakdownAttachment(
                id, draftId, fileName, contentType, storageKey, sizeBytes, createdByUserId, createdAt, deletedAt);
    }
}
