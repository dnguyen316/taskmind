package com.taskmind.backend.analytics.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AnalyticsRollupRepository {
    List<ReportsTrend> userTrends(UUID userId, LocalDate start);

    List<ReportsProjectThroughput> projectThroughput(LocalDate start);

    List<ReportsStatusSegment> statusSegments(UUID userId);

    List<ReportsAssigneeWorkload> assigneeWorkload();
}
