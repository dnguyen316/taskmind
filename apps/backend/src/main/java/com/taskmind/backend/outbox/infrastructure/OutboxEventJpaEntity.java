package com.taskmind.backend.outbox.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventJpaEntity {
    @Id private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "stream_key", nullable = false)
    private String streamKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "publish_attempts", nullable = false)
    private int publishAttempts;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OutboxEventJpaEntity() {}

    public OutboxEventJpaEntity(
            UUID id,
            UUID eventId,
            String eventType,
            String streamKey,
            String payload,
            Instant occurredAt,
            Instant now) {
        this.id = id;
        this.eventId = eventId;
        this.eventType = eventType;
        this.streamKey = streamKey;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID eventId() {
        return eventId;
    }

    public String eventType() {
        return eventType;
    }

    public String streamKey() {
        return streamKey;
    }

    public String payload() {
        return payload;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public Instant publishedAt() {
        return publishedAt;
    }

    public int publishAttempts() {
        return publishAttempts;
    }

    public void markPublished(Instant time) {
        this.publishedAt = time;
        this.lastError = null;
        this.updatedAt = time;
    }

    public void markPublishFailed(String error, Instant time) {
        this.publishAttempts++;
        this.lastError = error;
        this.updatedAt = time;
    }
}
