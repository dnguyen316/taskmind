package com.taskmind.backend.dashboard.application;

public record KpiMetrics(
        int openTasks, int completedTasks, int eventsIngested, double completionRate) {}
