package com.taskmind.events;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record DomainEvent(
        UUID eventId,
        int schemaVersion,
        String eventType,
        Instant occurredAt,
        UUID actorUserId,
        Scope scope,
        EntityRef entity,
        JsonNode payload,
        JsonNode context) {
    public record Scope(String tenantId, UUID userId, UUID projectId) {}

    public record EntityRef(String type, UUID id) {}
}
