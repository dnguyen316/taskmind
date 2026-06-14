package com.taskmind.backend.ai.application;

public record DurationEstimateResult(int durationMinutes, String rationale, double confidence) {}
