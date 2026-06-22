package com.taskmind.relay.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskmind.events.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ActivityEventDocument(
        UUID eventId,
        String eventType,
        UUID actorUserId,
        UUID userId,
        UUID projectId,
        String entityType,
        String entityTypeKeyword,
        UUID entityId,
        String title,
        String status,
        String statusKeyword,
        String eventTypeKeyword,
        JsonNode payload,
        String payloadText,
        Instant occurredAt) {
    private static final List<String> TITLE_FIELDS =
            List.of("title", "name", "fileName", "filename", "displayName", "summary");
    private static final List<String> STATUS_FIELDS = List.of("status", "state", "lifecycle", "archived");

    public static ActivityEventDocument from(DomainEvent event) {
        return new ActivityEventDocument(
                event.eventId(),
                event.eventType(),
                event.actorUserId(),
                event.scope().userId(),
                event.scope().projectId(),
                event.entity().type(),
                normalizeKeyword(event.entity().type()),
                event.entity().id(),
                firstText(event.payload(), TITLE_FIELDS),
                firstText(event.payload(), STATUS_FIELDS),
                normalizeKeyword(firstText(event.payload(), STATUS_FIELDS)),
                normalizeKeyword(event.eventType()),
                event.payload(),
                searchablePayloadText(event.payload()),
                event.occurredAt());
    }

    private static String firstText(JsonNode payload, List<String> fieldNames) {
        if (payload == null || payload.isMissingNode() || payload.isNull()) {
            return "";
        }
        for (String fieldName : fieldNames) {
            JsonNode value = payload.path(fieldName);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = value.isTextual() ? value.asText() : value.asText("");
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }

    private static String normalizeKeyword(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String searchablePayloadText(JsonNode payload) {
        if (payload == null || payload.isMissingNode() || payload.isNull()) {
            return "";
        }
        return payload.toString();
    }
}
