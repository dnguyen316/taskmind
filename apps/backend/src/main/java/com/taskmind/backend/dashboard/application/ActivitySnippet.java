package com.taskmind.backend.dashboard.application;

import java.time.LocalDate;

public record ActivitySnippet(
        LocalDate date, int tasksCreated, int tasksCompleted, int eventsIngested) {}
