package com.taskmind.backend.ai.application;

import java.util.Map;
import java.util.UUID;

public interface AiDomainEventPublisher {
    void publish(UUID userId, String eventType, Map<String, Object> payload);
}
