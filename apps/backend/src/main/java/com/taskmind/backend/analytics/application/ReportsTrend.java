package com.taskmind.backend.analytics.application;

import java.time.LocalDate;

public record ReportsTrend(
        LocalDate date, int tasksCreated, int tasksCompleted, int eventsIngested) {}
