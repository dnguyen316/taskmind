package com.taskmind.backend.activity.search;

import java.time.Instant;
import java.util.UUID;

public record ActivitySearchRequest(
        UUID userId,
        String query,
        int size,
        String entityType,
        String status,
        UUID projectId,
        Instant from,
        Instant to,
        String eventType) {}
