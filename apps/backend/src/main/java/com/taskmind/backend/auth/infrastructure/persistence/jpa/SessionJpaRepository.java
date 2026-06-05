package com.taskmind.backend.auth.infrastructure.persistence.jpa;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionJpaRepository extends JpaRepository<SessionJpaEntity, UUID> {
    List<SessionJpaEntity> findByUser_Id(UUID userId);
    Optional<SessionJpaEntity> findByRefreshTokenHash(String refreshTokenHash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessionJpaEntity s where s.refreshTokenHash = :hash")
    Optional<SessionJpaEntity> findLockedByRefreshTokenHash(@Param("hash") String refreshTokenHash);
    List<SessionJpaEntity> findByExpiresAtBefore(Instant cutoff);
}
