package com.taskmind.backend.project.infrastructure.persistence.jpa;

import com.taskmind.backend.project.domain.model.ProjectMembership;
import com.taskmind.backend.project.domain.model.ProjectMembershipRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "project_memberships")
@IdClass(ProjectMembershipJpaId.class)
public class ProjectMembershipJpaEntity {

    @Id
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectMembershipRole role;

    protected ProjectMembershipJpaEntity() {
    }

    private ProjectMembershipJpaEntity(ProjectMembership membership) {
        this.projectId = membership.projectId();
        this.userId = membership.userId();
        this.role = membership.role();
    }

    public static ProjectMembershipJpaEntity fromDomain(ProjectMembership membership) {
        return new ProjectMembershipJpaEntity(membership);
    }

    public ProjectMembership toDomain() {
        return new ProjectMembership(projectId, userId, role);
    }
}
