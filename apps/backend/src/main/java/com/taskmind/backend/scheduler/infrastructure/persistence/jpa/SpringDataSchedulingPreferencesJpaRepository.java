package com.taskmind.backend.scheduler.infrastructure.persistence.jpa;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataSchedulingPreferencesJpaRepository
        extends JpaRepository<SchedulingPreferencesJpaEntity, UUID> {
    Optional<SchedulingPreferencesJpaEntity> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SchedulingPreferencesJpaEntity p where p.userId=:userId")
    Optional<SchedulingPreferencesJpaEntity> findByUserIdForUpdate(@Param("userId") UUID userId);
}
