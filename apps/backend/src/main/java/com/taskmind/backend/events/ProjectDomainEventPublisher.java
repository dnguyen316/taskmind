package com.taskmind.backend.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskmind.backend.activity.application.ActivityEventRecorder;
import com.taskmind.backend.activity.domain.model.ActivityEventType;
import com.taskmind.backend.outbox.application.OutboxEventWriter;
import com.taskmind.backend.project.domain.model.Project;
import com.taskmind.events.DomainEvent;
import com.taskmind.events.EventTypes;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProjectDomainEventPublisher {
    private final OutboxEventWriter outbox;
    private final ActivityEventRecorder activity;
    private final ObjectMapper objectMapper;

    public ProjectDomainEventPublisher(
            OutboxEventWriter outbox, ActivityEventRecorder activity, ObjectMapper objectMapper) {
        this.outbox = outbox;
        this.activity = activity;
        this.objectMapper = objectMapper;
    }

    public void projectCreated(Project project) {
        append(
                event(EventTypes.PROJECT_CREATED, project, project.ownerUserId()),
                ActivityEventType.PROJECT_CREATED);
    }

    public void projectUpdated(Project project, UUID actorUserId) {
        append(
                event(EventTypes.PROJECT_UPDATED, project, actorUserId),
                ActivityEventType.PROJECT_UPDATED);
    }

    public void projectArchived(Project project, UUID actorUserId) {
        append(
                event(EventTypes.PROJECT_ARCHIVED, project, actorUserId),
                ActivityEventType.PROJECT_ARCHIVED);
    }

    private DomainEvent event(String type, Project project, UUID actorUserId) {
        Instant now = Instant.now();
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("projectId", project.id().toString());
        payload.put("ownerUserId", project.ownerUserId().toString());
        payload.put("name", project.name());
        payload.put("key", project.key());
        payload.put("archived", project.archivedAt() != null);
        return new DomainEvent(
                UUID.randomUUID(),
                1,
                type,
                now,
                actorUserId,
                new DomainEvent.Scope("default", project.ownerUserId(), project.id()),
                new DomainEvent.EntityRef("project", project.id()),
                payload,
                objectMapper.createObjectNode());
    }

    private void append(DomainEvent event, ActivityEventType type) {
        outbox.append(event);
        activity.record(event, type);
    }
}
