package com.taskmind.backend.auth.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIdentityJpaRepository extends JpaRepository<UserIdentityJpaEntity, UUID> {
    Optional<UserIdentityJpaEntity> findByTypeAndValue(AuthJpaEnums.IdentityType type, String value);
}
