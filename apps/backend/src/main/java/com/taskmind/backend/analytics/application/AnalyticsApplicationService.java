package com.taskmind.backend.analytics.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsApplicationService {
    private final AnalyticsRollupRepository repository;
    private final Clock clock;

    public AnalyticsApplicationService(AnalyticsRollupRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public ReportsResponse reports(AuthenticatedUser requester, ReportsRange range) {
        LocalDate start = range.start(LocalDate.now(clock));
        List<ReportsTrend> trends = repository.userTrends(requester.userId(), start);
        List<ReportsAssigneeWorkload> assigneeWorkload = repository.assigneeWorkload();
        int created = trends.stream().mapToInt(ReportsTrend::tasksCreated).sum();
        int completed = trends.stream().mapToInt(ReportsTrend::tasksCompleted).sum();
        int events = trends.stream().mapToInt(ReportsTrend::eventsIngested).sum();
        int projectsCreated = repository.projectsCreated(requester.userId(), start);
        ReportsKpis kpis =
                new ReportsKpis(
                        created,
                        completed,
                        projectsCreated,
                        events,
                        created == 0 ? 0.0 : (double) completed / created);
        return new ReportsResponse(
                range,
                List.of(
                        "tasksCreated",
                        "tasksCompleted",
                        "projectsCreated",
                        "eventsIngested",
                        "completionRate",
                        "statusSegments",
                        "projectThroughput",
                        "assigneeThroughput",
                        "assigneeWorkload",
                        "teamWorkload"),
                "Analytics rollups reflect Relay projections available in Core; priority segments are coming soon because task priority is not projected yet.",
                kpis,
                new ReportsDeltas(created, completed, events),
                new ReportsSparklines(
                        trends.stream().map(ReportsTrend::tasksCreated).toList(),
                        trends.stream().map(ReportsTrend::tasksCompleted).toList(),
                        trends.stream().map(ReportsTrend::eventsIngested).toList()),
                trends,
                repository.statusSegments(requester.userId()),
                List.of(),
                repository.projectThroughput(start),
                repository.assigneeThroughput(start),
                assigneeWorkload,
                new ReportsTeamWorkload(
                        assigneeWorkload.size(),
                        assigneeWorkload.stream().mapToInt(ReportsAssigneeWorkload::openTasks).sum()));
    }
}
