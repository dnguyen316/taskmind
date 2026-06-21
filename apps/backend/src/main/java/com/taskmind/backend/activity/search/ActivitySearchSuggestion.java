package com.taskmind.backend.activity.search;

import java.time.Instant;
import java.util.UUID;

public record ActivitySearchSuggestion(
        String label,
        String value,
        String entityType,
        UUID entityId,
        String eventType,
        String status,
        String title,
        Instant occurredAt,
        String routeName) {}
