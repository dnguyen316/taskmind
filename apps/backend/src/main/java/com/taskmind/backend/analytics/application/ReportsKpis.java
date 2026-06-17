package com.taskmind.backend.analytics.application;

public record ReportsKpis(
        int tasksCreated,
        int tasksCompleted,
        int projectsCreated,
        int eventsIngested,
        double completionRate) {}
