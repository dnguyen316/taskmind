package com.taskmind.backend.scheduler.infrastructure.persistence.jpa;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataScheduledBlockJpaRepository
        extends JpaRepository<ScheduledBlockJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from ScheduledBlockJpaEntity b where b.id=:id and b.deletedAt is null")
    Optional<ScheduledBlockJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("select b from ScheduledBlockJpaEntity b where b.id=:id and b.deletedAt is null")
    Optional<ScheduledBlockJpaEntity> findActiveById(@Param("id") UUID id);

    @Query(
            "select b from ScheduledBlockJpaEntity b where b.userId=:userId and b.deletedAt is null and b.startsAt>=:from and b.startsAt<:to order by b.startsAt")
    List<ScheduledBlockJpaEntity> findByUserIdBetween(
            @Param("userId") UUID userId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}
