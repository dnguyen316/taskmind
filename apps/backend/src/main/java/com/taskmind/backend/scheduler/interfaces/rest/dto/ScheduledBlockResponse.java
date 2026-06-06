package com.taskmind.backend.scheduler.interfaces.rest.dto;

import com.taskmind.backend.scheduler.domain.model.ScheduledBlock;
import com.taskmind.backend.scheduler.domain.model.ScheduledBlockStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduledBlockResponse(
        UUID id,
        Long version,
        UUID userId,
        UUID taskId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        ScheduledBlockStatus status,
        String rationale,
        OffsetDateTime completedAt,
        Instant createdAt,
        Instant updatedAt) {
    public static ScheduledBlockResponse fromDomain(ScheduledBlock block) {
        return new ScheduledBlockResponse(
                block.id(),
                block.version(),
                block.userId(),
                block.taskId(),
                block.startsAt(),
                block.endsAt(),
                block.status(),
                block.rationale(),
                block.completedAt(),
                block.createdAt(),
                block.updatedAt());
    }
}
