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
        List<ReportsTrend> trends =
                repository.userTrends(requester.userId(), range.start(LocalDate.now(clock)));
        int created = trends.stream().mapToInt(ReportsTrend::tasksCreated).sum();
        int completed = trends.stream().mapToInt(ReportsTrend::tasksCompleted).sum();
        int events = trends.stream().mapToInt(ReportsTrend::eventsIngested).sum();
        ReportsKpis kpis =
                new ReportsKpis(
                        created,
                        completed,
                        0,
                        events,
                        created == 0 ? 0.0 : (double) completed / created);
        return new ReportsResponse(
                range,
                kpis,
                new ReportsDeltas(created, completed, events),
                new ReportsSparklines(
                        trends.stream().map(ReportsTrend::tasksCreated).toList(),
                        trends.stream().map(ReportsTrend::tasksCompleted).toList(),
                        trends.stream().map(ReportsTrend::eventsIngested).toList()),
                trends,
                repository.statusSegments(requester.userId()),
                List.of(),
                repository.projectThroughput(range.start(LocalDate.now(clock))),
                List.of(new ReportsAssigneeThroughput(requester.userId(), created, completed)),
                repository.assigneeWorkload(),
                new ReportsTeamWorkload(
                        repository.assigneeWorkload().size(),
                        repository.assigneeWorkload().stream()
                                .mapToInt(ReportsAssigneeWorkload::openTasks)
                                .sum()));
    }
}
