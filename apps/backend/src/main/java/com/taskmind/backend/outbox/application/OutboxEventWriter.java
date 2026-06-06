package com.taskmind.backend.outbox.application;

import com.taskmind.backend.outbox.infrastructure.OutboxEventJpaEntity;
import com.taskmind.backend.outbox.infrastructure.OutboxEventJpaRepository;
import com.taskmind.events.DomainEvent;
import com.taskmind.events.DomainEventMapper;
import com.taskmind.events.DomainEventValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OutboxEventWriter {
    private final OutboxEventJpaRepository repository;
    private final DomainEventMapper mapper;
    private final DomainEventValidator validator;
    private final String streamKey;

    public OutboxEventWriter(
            OutboxEventJpaRepository repository,
            @Value("${taskmind.relay.stream-key:taskmind.events}") String streamKey) {
        this.repository = repository;
        this.mapper = new DomainEventMapper();
        this.validator = new DomainEventValidator();
        this.streamKey = streamKey;
    }

    public void append(DomainEvent event) {
        validator.validate(event);
        repository.save(
                new OutboxEventJpaEntity(
                        event.eventId(),
                        event.eventId(),
                        event.eventType(),
                        streamKey,
                        mapper.toJson(event),
                        event.occurredAt(),
                        event.occurredAt()));
    }
}
