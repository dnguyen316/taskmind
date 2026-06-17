package com.taskmind.backend.dashboard.application;

import java.util.List;

public record DashboardResponse(
        KpiMetrics kpis, List<MyTaskItem> myTasks, List<ActivitySnippet> activity) {}
