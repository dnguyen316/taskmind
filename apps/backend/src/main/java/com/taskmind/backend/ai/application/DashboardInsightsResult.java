package com.taskmind.backend.ai.application;

import java.util.List;
import java.util.UUID;

public record DashboardInsightsResult(UUID userId, String summary, List<String> insights, List<String> recommendations) {}
