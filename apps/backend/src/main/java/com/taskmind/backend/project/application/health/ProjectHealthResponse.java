package com.taskmind.backend.project.application.health;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectHealthResponse(
        UUID projectId,
        int totalTaskCount,
        int completedTaskCount,
        int completionPercentage,
        int overdueTaskCount,
        int blockedTaskCount,
        int unassignedTaskCount,
        int staleTaskCount,
        int upcomingDeadlineRiskCount,
        List<AssigneeWorkload> workloadByAssignee,
        String narrative,
        Instant calculatedAt) {

    public record AssigneeWorkload(UUID assigneeId, int activeTaskCount) {}
}
