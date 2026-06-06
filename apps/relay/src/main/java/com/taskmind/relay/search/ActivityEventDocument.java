package com.taskmind.relay.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskmind.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ActivityEventDocument(
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
        String payloadText,
        Instant occurredAt) {
    public static ActivityEventDocument from(DomainEvent event) {
        return new ActivityEventDocument(
                event.eventId(),
                event.eventType(),
                event.actorUserId(),
                event.scope().userId(),
                event.scope().projectId(),
                event.entity().type(),
                event.entity().id(),
                event.payload().path("title").asText(""),
                event.payload().path("status").asText(""),
                event.payload(),
                event.payload().toString(),
                event.occurredAt());
    }
}
