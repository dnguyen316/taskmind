package com.taskmind.backend.scheduler.domain.repository;

import com.taskmind.backend.scheduler.domain.model.ScheduledBlock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduledBlockRepository {
    ScheduledBlock save(ScheduledBlock block);

    List<ScheduledBlock> saveAll(List<ScheduledBlock> blocks);

    Optional<ScheduledBlock> findById(UUID id);

    Optional<ScheduledBlock> findByIdForUpdate(UUID id);

    List<ScheduledBlock> findByUserIdBetween(UUID userId, OffsetDateTime from, OffsetDateTime to);
}
