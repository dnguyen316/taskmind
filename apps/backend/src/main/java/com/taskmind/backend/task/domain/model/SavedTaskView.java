package com.taskmind.backend.task.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SavedTaskView(
        UUID id,
        Long version,
        UUID userId,
        String name,
        String filtersJson,
        boolean builtIn,
        Instant createdAt,
        Instant updatedAt) {}
