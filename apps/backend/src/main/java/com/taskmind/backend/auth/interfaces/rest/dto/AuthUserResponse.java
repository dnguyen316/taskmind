package com.taskmind.backend.auth.interfaces.rest.dto;

import java.util.UUID;

public record AuthUserResponse(UUID userId, String email, String displayName, boolean onboardingCompleted, String onboardingWorkspaceType, String onboardingPlanningStyle) {
}
