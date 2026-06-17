package com.taskmind.backend.analytics.application;

import java.util.UUID;

public record ReportsAssigneeThroughput(UUID userId, int tasksCreated, int tasksCompleted) {}
