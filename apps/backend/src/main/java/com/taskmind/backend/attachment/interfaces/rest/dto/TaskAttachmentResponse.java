package com.taskmind.backend.attachment.interfaces.rest.dto;

import com.taskmind.backend.attachment.domain.model.MediaKind;
import java.time.Instant;
import java.util.UUID;

public class TaskAttachmentResponse {
    public UUID id;
    public Long version;
    public UUID taskId;
    public UUID ownerUserId;
    public String fileName;
    public String contentType;
    public long sizeBytes;
    public MediaKind mediaKind;
    public Instant deletedAt;
    public Instant createdAt;
    public Instant updatedAt;
}
