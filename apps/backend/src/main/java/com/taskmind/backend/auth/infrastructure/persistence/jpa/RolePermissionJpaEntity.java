package com.taskmind.backend.auth.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "role_permissions")
public class RolePermissionJpaEntity {

    @EmbeddedId private RolePermissionJpaId id;

    @ManyToOne(optional = false)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    private RoleJpaEntity role;

    @ManyToOne(optional = false)
    @MapsId("permissionId")
    @JoinColumn(name = "permission_id", nullable = false)
    private PermissionJpaEntity permission;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    protected RolePermissionJpaEntity() {}
}
