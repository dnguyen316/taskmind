package com.taskmind.relay.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskmind.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActivityEventDocumentTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void derivesProjectTitleAndStatusFromProjectPayloadShape() {
        ObjectNode payload = mapper.createObjectNode()
                .put("projectId", UUID.randomUUID().toString())
                .put("name", "Platform Roadmap")
                .put("archived", false);

        ActivityEventDocument document = ActivityEventDocument.from(event("project.updated", "project", payload));

        assertEquals("Platform Roadmap", document.title());
        assertEquals("false", document.status());
        assertTrue(document.payloadText().contains("Platform Roadmap"));
    }

    @Test
    void derivesAttachmentTitleAndSearchableTextFromAttachmentPayloadShape() {
        ObjectNode payload = mapper.createObjectNode()
                .put("fileName", "requirements.pdf")
                .put("contentType", "application/pdf")
                .put("mediaKind", "DOCUMENT");

        ActivityEventDocument document = ActivityEventDocument.from(event("attachment.uploaded", "attachment", payload));

        assertEquals("requirements.pdf", document.title());
        assertEquals("", document.status());
        assertTrue(document.payloadText().contains("application/pdf"));
    }

    private DomainEvent event(String eventType, String entityType, com.fasterxml.jackson.databind.JsonNode payload) {
        UUID userId = UUID.randomUUID();
        return new DomainEvent(
                UUID.randomUUID(),
                1,
                eventType,
                Instant.parse("2026-01-01T00:00:00Z"),
                userId,
                new DomainEvent.Scope("default", userId, UUID.randomUUID()),
                new DomainEvent.EntityRef(entityType, UUID.randomUUID()),
                payload,
                mapper.createObjectNode());
    }
}
