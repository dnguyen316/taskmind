package com.taskmind.backend.team.application;

import com.taskmind.backend.project.domain.model.ProjectMembershipRole;
import java.util.UUID;

public record ChangeProjectMemberRoleCommand(UUID userId, UUID projectId, ProjectMembershipRole role) {}
