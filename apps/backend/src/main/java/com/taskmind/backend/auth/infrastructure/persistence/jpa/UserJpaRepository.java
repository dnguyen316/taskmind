package com.taskmind.backend.auth.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByPrimaryEmail(String primaryEmail);

    Optional<UserJpaEntity> findByPrimaryPhone(String primaryPhone);
}
