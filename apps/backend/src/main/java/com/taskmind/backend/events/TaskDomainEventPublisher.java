package com.taskmind.backend.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskmind.backend.activity.application.ActivityEventRecorder;
import com.taskmind.backend.activity.domain.model.ActivityEventType;
import com.taskmind.backend.outbox.application.OutboxEventWriter;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskStatus;
import com.taskmind.events.DomainEvent;
import com.taskmind.events.EventTypes;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TaskDomainEventPublisher {
    private final OutboxEventWriter outbox;
    private final ActivityEventRecorder activity;
    private final ObjectMapper objectMapper;

    public TaskDomainEventPublisher(
            OutboxEventWriter outbox, ActivityEventRecorder activity, ObjectMapper objectMapper) {
        this.outbox = outbox;
        this.activity = activity;
        this.objectMapper = objectMapper;
    }

    public void taskCreated(Task task, UUID actorUserId) {
        DomainEvent event = event(EventTypes.TASK_CREATED, task, actorUserId, payload(task));
        append(event, ActivityEventType.TASK_CREATED);
    }

    public void taskUpdated(Task previous, Task current, UUID actorUserId) {
        String type =
                current.status() != previous.status()
                        ? EventTypes.TASK_STATUS_CHANGED
                        : EventTypes.TASK_UPDATED;
        ObjectNode payload = payload(current);
        payload.put("previousStatus", previous.status().name());
        DomainEvent event = event(type, current, actorUserId, payload);
        append(
                event,
                type.equals(EventTypes.TASK_STATUS_CHANGED)
                        ? ActivityEventType.TASK_STATUS_CHANGED
                        : ActivityEventType.TASK_UPDATED);
    }

    private DomainEvent event(String type, Task task, UUID actorUserId, ObjectNode payload) {
        Instant now = Instant.now();
        return new DomainEvent(
                UUID.randomUUID(),
                1,
                type,
                now,
                actorUserId,
                new DomainEvent.Scope("default", task.userId(), task.projectId()),
                new DomainEvent.EntityRef("task", task.id()),
                payload,
                objectMapper.createObjectNode());
    }

    private ObjectNode payload(Task task) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("taskId", task.id().toString());
        payload.put("userId", task.userId().toString());
        if (task.projectId() != null) {
            payload.put("projectId", task.projectId().toString());
        }
        payload.put("title", task.title());
        TaskStatus status = task.status();
        payload.put("status", status.name());
        if (task.assigneeId() != null) {
            payload.put("assigneeId", task.assigneeId().toString());
        }
        return payload;
    }

    private void append(DomainEvent event, ActivityEventType type) {
        outbox.append(event);
        activity.record(event, type);
    }
}
