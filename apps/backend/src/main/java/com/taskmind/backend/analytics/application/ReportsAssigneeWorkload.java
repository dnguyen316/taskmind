package com.taskmind.backend.analytics.application;

import java.util.UUID;

public record ReportsAssigneeWorkload(UUID userId, int openTasks) {}
