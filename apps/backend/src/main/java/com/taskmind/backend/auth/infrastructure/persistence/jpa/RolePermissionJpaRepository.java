package com.taskmind.backend.auth.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionJpaEntity, RolePermissionJpaId> {

    List<RolePermissionJpaEntity> findByIdRoleId(UUID roleId);
}
