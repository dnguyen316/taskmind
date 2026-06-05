package com.taskmind.backend.auth.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpChallengeJpaRepository extends JpaRepository<OtpChallengeJpaEntity, UUID> {
    List<OtpChallengeJpaEntity> findByDestinationAndExpiresAtAfter(String destination, Instant now);
    Optional<OtpChallengeJpaEntity> findFirstByDestinationAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(String destination, Instant now);
    List<OtpChallengeJpaEntity> findByExpiresAtBefore(Instant cutoff);
}
