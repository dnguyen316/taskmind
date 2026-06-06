package com.taskmind.backend.activity.application;

import com.taskmind.backend.activity.domain.model.ActivityEvent;
import com.taskmind.backend.activity.domain.model.ActivityEventType;
import com.taskmind.backend.activity.domain.repository.ActivityEventRepository;
import com.taskmind.events.DomainEvent;
import com.taskmind.events.DomainEventMapper;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ActivityEventRecorder {
    private final ActivityEventRepository repository;
    private final DomainEventMapper mapper = new DomainEventMapper();

    public ActivityEventRecorder(ActivityEventRepository repository) {
        this.repository = repository;
    }

    public void record(DomainEvent event, ActivityEventType type) {
        repository.save(
                new ActivityEvent(
                        UUID.randomUUID(),
                        event.eventId(),
                        type,
                        event.actorUserId(),
                        event.entity().type(),
                        event.entity().id(),
                        event.scope().projectId(),
                        event.occurredAt(),
                        event.payload().toString(),
                        event.context().toString(),
                        event.occurredAt()));
    }

    public String serialize(DomainEvent event) {
        return mapper.toJson(event);
    }
}
