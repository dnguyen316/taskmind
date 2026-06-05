package com.taskmind.backend.project.domain.model;

import java.util.UUID;

public record ProjectMembership(
    UUID projectId,
    UUID userId,
    ProjectMembershipRole role
) {
}
