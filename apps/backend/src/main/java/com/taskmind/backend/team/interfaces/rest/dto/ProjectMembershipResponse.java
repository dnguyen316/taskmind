package com.taskmind.backend.team.interfaces.rest.dto;

import com.taskmind.backend.project.domain.model.ProjectMembership;
import com.taskmind.backend.project.domain.model.ProjectMembershipRole;
import java.util.UUID;

public record ProjectMembershipResponse(UUID projectId, UUID userId, ProjectMembershipRole role) {
    public static ProjectMembershipResponse from(ProjectMembership membership) {
        return new ProjectMembershipResponse(membership.projectId(), membership.userId(), membership.role());
    }
}
