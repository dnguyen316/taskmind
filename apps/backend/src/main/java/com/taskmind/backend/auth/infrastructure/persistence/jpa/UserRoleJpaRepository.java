package com.taskmind.backend.auth.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleJpaEntity, UserRoleJpaId> {

    List<UserRoleJpaEntity> findByIdUserId(UUID userId);

    @Query("""
            select distinct permission.name
            from UserRoleJpaEntity userRole
            join userRole.role role
            join RolePermissionJpaEntity rolePermission on rolePermission.role = role
            join rolePermission.permission permission
            where userRole.id.userId = :userId
            """)
    List<String> findPermissionNamesByUserId(@Param("userId") UUID userId);
}
