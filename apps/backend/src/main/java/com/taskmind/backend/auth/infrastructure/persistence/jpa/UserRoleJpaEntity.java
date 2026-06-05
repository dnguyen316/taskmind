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
@Table(name = "user_roles")
public class UserRoleJpaEntity {

    @EmbeddedId
    private UserRoleJpaId id;

    @ManyToOne(optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @ManyToOne(optional = false)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    private RoleJpaEntity role;

    @ManyToOne
    @JoinColumn(name = "assigned_by_user_id")
    private UserJpaEntity assignedByUser;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected UserRoleJpaEntity() {
    }

    public UserRoleJpaEntity(UserJpaEntity user, RoleJpaEntity role, Instant assignedAt) {
        this.id = new UserRoleJpaId(user.getId(), role.getId()); this.user = user; this.role = role; this.assignedAt = assignedAt;
    }

    public RoleJpaEntity getRole() { return role; }
}
