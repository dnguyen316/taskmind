package com.taskmind.backend.attachment.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TaskAttachment(
        UUID id,
        Long version,
        UUID taskId,
        UUID ownerUserId,
        String objectKey,
        String fileName,
        String contentType,
        long sizeBytes,
        MediaKind mediaKind,
        Instant deletedAt,
        Instant createdAt,
        Instant updatedAt) {}
