package com.taskmind.backend.aitaskresolution.infra;

import com.taskmind.backend.aitaskresolution.domain.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAiTaskResolutionJobRepository implements AiTaskResolutionJobRepository {
    private final JdbcTemplate jdbc;

    public JdbcAiTaskResolutionJobRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public AiTaskResolutionJob save(AiTaskResolutionJob job) {
        jdbc.update("""
                INSERT INTO ai_task_resolution_jobs (id, task_id, project_id, template_id, github_project_link_id, status, requested_by, idempotency_key, nova_run_id, current_step, result_summary, error_code, created_at, updated_at, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, nova_run_id = EXCLUDED.nova_run_id, current_step = EXCLUDED.current_step, result_summary = EXCLUDED.result_summary, error_code = EXCLUDED.error_code, updated_at = EXCLUDED.updated_at, completed_at = EXCLUDED.completed_at
                """, job.id(), job.taskId(), job.projectId(), job.templateId(), job.githubProjectLinkId(), job.status().name(), job.requestedBy(), job.idempotencyKey(), job.novaRunId(), job.currentStep(), job.resultSummary(), job.errorCode(), job.createdAt(), job.updatedAt(), job.completedAt());
        return findById(job.id()).orElseThrow();
    }

    @Override public Optional<AiTaskResolutionJob> findById(UUID id) { return queryOne("SELECT * FROM ai_task_resolution_jobs WHERE id = ?", id); }

    @Override
    public Optional<AiTaskResolutionJob> findByTaskAndRequesterAndIdempotencyKey(UUID taskId, UUID requestedBy, String key) {
        return queryOne("SELECT * FROM ai_task_resolution_jobs WHERE task_id = ? AND requested_by = ? AND idempotency_key = ?", taskId, requestedBy, key);
    }

    @Override
    public List<AiTaskResolutionJob> findByTaskId(UUID taskId) {
        return jdbc.query("SELECT * FROM ai_task_resolution_jobs WHERE task_id = ? ORDER BY created_at DESC", this::map, taskId);
    }

    @Override
    public Optional<AiTaskResolutionJob> findFirstQueued() {
        return jdbc.query("SELECT * FROM ai_task_resolution_jobs WHERE status = 'QUEUED' ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED", this::map).stream().findFirst();
    }

    private Optional<AiTaskResolutionJob> queryOne(String sql, Object... args) { return jdbc.query(sql, this::map, args).stream().findFirst(); }
    private AiTaskResolutionJob map(ResultSet rs, int row) throws SQLException {
        return new AiTaskResolutionJob((UUID) rs.getObject("id"), (UUID) rs.getObject("task_id"), (UUID) rs.getObject("project_id"), (UUID) rs.getObject("template_id"), (UUID) rs.getObject("github_project_link_id"), AiTaskResolutionJobStatus.valueOf(rs.getString("status")), (UUID) rs.getObject("requested_by"), rs.getString("idempotency_key"), (UUID) rs.getObject("nova_run_id"), rs.getString("current_step"), rs.getString("result_summary"), rs.getString("error_code"), instant(rs, "created_at"), instant(rs, "updated_at"), instant(rs, "completed_at"));
    }
    private Instant instant(ResultSet rs, String column) throws SQLException { java.sql.Timestamp ts = rs.getTimestamp(column); return ts == null ? null : ts.toInstant(); }
}
