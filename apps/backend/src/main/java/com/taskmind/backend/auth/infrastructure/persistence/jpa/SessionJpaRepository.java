package com.taskmind.backend.auth.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionJpaRepository extends JpaRepository<SessionJpaEntity, UUID> {

    List<SessionJpaEntity> findByUser_Id(UUID userId);

    Optional<SessionJpaEntity> findByRefreshTokenHash(String refreshTokenHash);

    List<SessionJpaEntity> findByExpiresAtBefore(Instant cutoff);
}
