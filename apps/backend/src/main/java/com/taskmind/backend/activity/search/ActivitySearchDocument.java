package com.taskmind.backend.activity.search;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record ActivitySearchDocument(
        UUID eventId,
        String eventType,
        UUID actorUserId,
        UUID userId,
        UUID projectId,
        String entityType,
        UUID entityId,
        String title,
        String status,
        JsonNode payload,
        Instant occurredAt) {}
