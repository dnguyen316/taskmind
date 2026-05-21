package com.taskmind.backend.auth.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleJpaEntity, UserRoleJpaId> {

    List<UserRoleJpaEntity> findByIdUserId(UUID userId);
}
