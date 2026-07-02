package com.taskmind.backend.auth.application;

import java.util.UUID;

public record AuthUserView(
    UUID userId,
    String email,
    String displayName,
    boolean onboardingCompleted,
    String onboardingWorkspaceType,
    String onboardingPlanningStyle
) {
}
