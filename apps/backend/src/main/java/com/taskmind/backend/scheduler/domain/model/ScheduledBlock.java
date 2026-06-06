package com.taskmind.backend.scheduler.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduledBlock(
        UUID id,
        @JsonIgnore Long version,
        UUID userId,
        UUID taskId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        ScheduledBlockStatus status,
        String rationale,
        OffsetDateTime completedAt,
        Instant deletedAt,
        Instant createdAt,
        Instant updatedAt) {
    public ScheduledBlock {
        if (userId == null) throw new IllegalArgumentException("Scheduled block user is required");
        if (taskId == null) throw new IllegalArgumentException("Scheduled block task is required");
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("Scheduled block window is invalid");
        }
        if (status == null)
            throw new IllegalArgumentException("Scheduled block status is required");
    }

    public ScheduledBlock rescheduled(
            OffsetDateTime start, OffsetDateTime end, String reason, Instant now) {
        return new ScheduledBlock(
                id,
                version,
                userId,
                taskId,
                start,
                end,
                status,
                reason,
                completedAt,
                deletedAt,
                createdAt,
                now);
    }

    public ScheduledBlock completed(OffsetDateTime completedTime, Instant now) {
        return withStatus(ScheduledBlockStatus.COMPLETED, completedTime, now);
    }

    public ScheduledBlock missed(Instant now) {
        return withStatus(ScheduledBlockStatus.MISSED, completedAt, now);
    }

    public boolean shouldMarkMissed(OffsetDateTime now) {
        return status == ScheduledBlockStatus.SCHEDULED && completedAt == null && endsAt.isBefore(now);
    }

    private ScheduledBlock withStatus(
            ScheduledBlockStatus nextStatus, OffsetDateTime nextCompletedAt, Instant now) {
        return new ScheduledBlock(
                id,
                version,
                userId,
                taskId,
                startsAt,
                endsAt,
                nextStatus,
                rationale,
                nextCompletedAt,
                deletedAt,
                createdAt,
                now);
    }
}
