package com.taskmind.backend.analytics.infrastructure;

import com.taskmind.backend.analytics.application.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAnalyticsRollupRepository implements AnalyticsRollupRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAnalyticsRollupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ReportsTrend> userTrends(UUID userId, LocalDate start) {
        return jdbcTemplate.query(
                "select metric_date,tasks_created,tasks_completed,events_ingested from analytics.user_daily_metrics where user_id=? and metric_date>=? order by metric_date",
                (rs, i) ->
                        new ReportsTrend(
                                rs.getDate(1).toLocalDate(),
                                rs.getInt(2),
                                rs.getInt(3),
                                rs.getInt(4)),
                userId,
                start);
    }

    public List<ReportsProjectThroughput> projectThroughput(LocalDate start) {
        return jdbcTemplate.query(
                "select m.project_id, coalesce(p.name, 'Unknown project'), sum(m.tasks_created), sum(m.tasks_completed) from analytics.project_daily_metrics m left join analytics.project_projection p on p.project_id=m.project_id where m.metric_date>=? group by m.project_id,p.name order by sum(m.tasks_completed) desc limit 10",
                (rs, i) ->
                        new ReportsProjectThroughput(
                                (UUID) rs.getObject(1),
                                rs.getString(2),
                                rs.getInt(3),
                                rs.getInt(4)),
                start);
    }

    public List<ReportsStatusSegment> statusSegments(UUID userId) {
        return jdbcTemplate.query(
                "select status,count(*) from analytics.task_projection where user_id=? group by status order by status",
                (rs, i) -> new ReportsStatusSegment(rs.getString(1), rs.getInt(2)),
                userId);
    }

    public List<ReportsAssigneeWorkload> assigneeWorkload() {
        return jdbcTemplate.query(
                "select user_id,count(*) from analytics.task_projection where status not in ('DONE','CANCELED') group by user_id order by count(*) desc limit 20",
                (rs, i) -> new ReportsAssigneeWorkload((UUID) rs.getObject(1), rs.getInt(2)));
    }
}
