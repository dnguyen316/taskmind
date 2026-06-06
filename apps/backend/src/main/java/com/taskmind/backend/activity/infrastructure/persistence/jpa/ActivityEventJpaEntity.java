package com.taskmind.backend.activity.infrastructure.persistence.jpa;

import com.taskmind.backend.activity.domain.model.ActivityEvent;
import com.taskmind.backend.activity.domain.model.ActivityEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activity_events")
public class ActivityEventJpaEntity {
    @Id private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private ActivityEventType eventType;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String context;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ActivityEventJpaEntity() {}

    private ActivityEventJpaEntity(ActivityEvent event) {
        this.id = event.id();
        this.eventId = event.eventId();
        this.eventType = event.eventType();
        this.actorUserId = event.actorUserId();
        this.entityType = event.entityType();
        this.entityId = event.entityId();
        this.projectId = event.projectId();
        this.occurredAt = event.occurredAt();
        this.payload = event.payload();
        this.context = event.context();
        this.createdAt = event.createdAt();
    }

    static ActivityEventJpaEntity fromDomain(ActivityEvent event) {
        return new ActivityEventJpaEntity(event);
    }

    ActivityEvent toDomain() {
        return new ActivityEvent(
                id,
                eventId,
                eventType,
                actorUserId,
                entityType,
                entityId,
                projectId,
                occurredAt,
                payload,
                context,
                createdAt);
    }
}
