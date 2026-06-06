package com.taskmind.backend.attachment.infrastructure.persistence.jpa;

import com.taskmind.backend.attachment.domain.model.MediaKind;
import com.taskmind.backend.attachment.domain.model.TaskAttachment;
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
@Table(name = "task_attachments")
public class TaskAttachmentJpaEntity {
    @Id private UUID id;
    @Version private Long version;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "object_key", nullable = false, unique = true)
    private String objectKey;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_kind", nullable = false)
    private MediaKind mediaKind;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskAttachmentJpaEntity() {}

    private TaskAttachmentJpaEntity(TaskAttachment attachment) {
        id = attachment.id();
        version = attachment.version();
        taskId = attachment.taskId();
        ownerUserId = attachment.ownerUserId();
        objectKey = attachment.objectKey();
        fileName = attachment.fileName();
        contentType = attachment.contentType();
        sizeBytes = attachment.sizeBytes();
        mediaKind = attachment.mediaKind();
        deletedAt = attachment.deletedAt();
        createdAt = attachment.createdAt();
        updatedAt = attachment.updatedAt();
    }

    static TaskAttachmentJpaEntity fromDomain(TaskAttachment attachment) {
        return new TaskAttachmentJpaEntity(attachment);
    }

    TaskAttachment toDomain() {
        return new TaskAttachment(
                id,
                version,
                taskId,
                ownerUserId,
                objectKey,
                fileName,
                contentType,
                sizeBytes,
                mediaKind,
                deletedAt,
                createdAt,
                updatedAt);
    }
}
