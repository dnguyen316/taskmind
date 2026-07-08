package com.taskmind.backend.team.interfaces.rest.dto;

import com.taskmind.backend.project.domain.model.ProjectMembershipRole;
import jakarta.validation.constraints.NotNull;

public record AssignProjectMemberRequest(@NotNull ProjectMembershipRole role) {}
