package com.taskmind.backend.project.interfaces.rest.dto;

import com.taskmind.backend.project.domain.model.ProjectMembershipRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddProjectMemberRequest(
    @NotNull UUID userId,
    @NotNull ProjectMembershipRole role
) {
}
