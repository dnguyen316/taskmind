package com.taskmind.backend.ai.application;

import java.util.List;
import java.util.UUID;

public record WeeklyReviewResult(
        UUID userId,
        String summary,
        List<String> slippageInsights,
        List<String> recommendations,
        List<String> nextWeekPriorities) {}
