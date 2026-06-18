package com.taskmind.backend.specbreakdown.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SpecBreakdownAttachment(
        UUID id,
        UUID draftId,
        String fileName,
        String contentType,
        String storageKey,
        long sizeBytes,
        UUID createdByUserId,
        Instant createdAt,
        Instant deletedAt) {}
