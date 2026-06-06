package com.taskmind.backend.activity.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ActivityEvent(
        UUID id,
        UUID eventId,
        ActivityEventType eventType,
        UUID actorUserId,
        String entityType,
        UUID entityId,
        UUID projectId,
        Instant occurredAt,
        String payload,
        String context,
        Instant createdAt) {}
