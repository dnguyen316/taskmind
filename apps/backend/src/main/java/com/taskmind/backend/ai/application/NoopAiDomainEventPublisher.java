package com.taskmind.backend.ai.application;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class NoopAiDomainEventPublisher implements AiDomainEventPublisher {
    @Override
    public void publish(UUID userId, String eventType, Map<String, Object> payload) {
        // Outbox-backed AI funnel events are wired in the full M08 persistence slice.
    }
}
