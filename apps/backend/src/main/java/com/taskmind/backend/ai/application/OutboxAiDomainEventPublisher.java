package com.taskmind.backend.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.outbox.application.OutboxEventWriter;
import com.taskmind.events.DomainEvent;
import com.taskmind.events.EventTypes;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OutboxAiDomainEventPublisher implements AiDomainEventPublisher {
    private static final Set<String> SUPPORTED_EVENT_TYPES =
            Set.of(
                    EventTypes.AI_CAPTURE_SUBMITTED,
                    EventTypes.AI_SUGGESTION_ACCEPTED,
                    EventTypes.AI_SUGGESTION_REJECTED,
                    EventTypes.AI_SPEC_BREAKDOWN_COMPLETED,
                    EventTypes.AI_SPEC_BREAKDOWN_FAILED);

    private final OutboxEventWriter outboxEventWriter;
    private final ObjectMapper objectMapper;

    OutboxAiDomainEventPublisher(OutboxEventWriter outboxEventWriter, ObjectMapper objectMapper) {
        this.outboxEventWriter = outboxEventWriter;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(UUID userId, String eventType, Map<String, Object> payload) {
        if (!SUPPORTED_EVENT_TYPES.contains(eventType)) {
            throw new IllegalArgumentException("Unsupported AI funnel event type: " + eventType);
        }
        UUID eventId = UUID.randomUUID();
        outboxEventWriter.append(
                new DomainEvent(
                        eventId,
                        1,
                        eventType,
                        Instant.now(),
                        userId,
                        new DomainEvent.Scope("default", userId, null),
                        new DomainEvent.EntityRef("ai", eventId),
                        objectMapper.valueToTree(payload),
                        objectMapper.createObjectNode()));
    }
}
