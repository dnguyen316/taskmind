package com.taskmind.backend.analytics.application;

import java.util.List;

public record ReportsResponse(
        ReportsRange range,
        ReportsKpis kpis,
        ReportsDeltas deltas,
        ReportsSparklines sparklines,
        List<ReportsTrend> trends,
        List<ReportsStatusSegment> statusSegments,
        List<ReportsPrioritySegment> prioritySegments,
        List<ReportsProjectThroughput> projectThroughput,
        List<ReportsAssigneeThroughput> assigneeThroughput,
        List<ReportsAssigneeWorkload> assigneeWorkload,
        ReportsTeamWorkload teamWorkload) {}
