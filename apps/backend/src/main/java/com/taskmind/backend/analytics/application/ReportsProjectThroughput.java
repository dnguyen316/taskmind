package com.taskmind.backend.analytics.application;

import java.util.UUID;

public record ReportsProjectThroughput(
        UUID projectId, String name, int tasksCreated, int tasksCompleted) {}
