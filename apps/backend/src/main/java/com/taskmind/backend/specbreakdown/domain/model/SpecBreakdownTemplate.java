package com.taskmind.backend.specbreakdown.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SpecBreakdownTemplate(
        UUID id,
        Long version,
        UUID projectId,
        String name,
        String description,
        String fields,
        Instant createdAt,
        Instant updatedAt) {}
