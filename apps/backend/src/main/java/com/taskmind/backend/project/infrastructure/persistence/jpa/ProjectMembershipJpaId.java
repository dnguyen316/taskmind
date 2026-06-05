package com.taskmind.backend.project.infrastructure.persistence.jpa;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ProjectMembershipJpaId implements Serializable {

    private UUID projectId;
    private UUID userId;

    public ProjectMembershipJpaId() {
    }

    public ProjectMembershipJpaId(UUID projectId, UUID userId) {
        this.projectId = projectId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProjectMembershipJpaId that = (ProjectMembershipJpaId) o;
        return Objects.equals(projectId, that.projectId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, userId);
    }
}
