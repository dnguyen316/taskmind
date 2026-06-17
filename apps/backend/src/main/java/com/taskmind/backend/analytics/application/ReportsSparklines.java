package com.taskmind.backend.analytics.application;

import java.util.List;

public record ReportsSparklines(
        List<Integer> tasksCreated, List<Integer> tasksCompleted, List<Integer> eventsIngested) {}
