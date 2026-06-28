package com.taskmind.backend.aitaskresolution.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskmind.backend.aitaskresolution.domain.AiTaskResolutionJob;
import com.taskmind.backend.outbox.application.OutboxEventWriter;
import com.taskmind.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AiTaskResolutionEventPublisher {
    private final OutboxEventWriter outbox;
    private final ObjectMapper mapper;

    public AiTaskResolutionEventPublisher(OutboxEventWriter outbox, ObjectMapper mapper) {
        this.outbox = outbox;
        this.mapper = mapper;
    }

    public void statusChanged(AiTaskResolutionJob job) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("jobId", job.id().toString());
        payload.put("taskId", job.taskId().toString());
        payload.put("status", job.status().name());
        if (job.currentStep() != null) payload.put("currentStep", job.currentStep());
        if (job.errorCode() != null) payload.put("errorCode", job.errorCode());
        outbox.append(
                new DomainEvent(
                        UUID.randomUUID(),
                        1,
                        "ai.task_resolution.status_changed",
                        Instant.now(),
                        job.requestedBy(),
                        new DomainEvent.Scope("default", job.requestedBy(), job.projectId()),
                        new DomainEvent.EntityRef("ai_task_resolution_job", job.id()),
                        payload,
                        mapper.createObjectNode()));
    }
}
