package com.taskmind.backend.aitaskresolution.infra;

import com.taskmind.backend.aitaskresolution.domain.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAiTaskResolutionActionProposalRepository
        implements AiTaskResolutionActionProposalRepository {
    private final JdbcTemplate jdbc;

    public JdbcAiTaskResolutionActionProposalRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public AiTaskResolutionActionProposal save(AiTaskResolutionActionProposal proposal) {
        jdbc.update(
                """
                INSERT INTO ai_task_resolution_action_proposals (id, job_id, proposed_action_type, payload_preview, risk_level, rationale, status, decided_by, decided_at, error_code, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, decided_by = EXCLUDED.decided_by, decided_at = EXCLUDED.decided_at, error_code = EXCLUDED.error_code, updated_at = EXCLUDED.updated_at
                """,
                proposal.id(),
                proposal.jobId(),
                proposal.proposedActionType(),
                proposal.payloadPreview(),
                proposal.riskLevel(),
                proposal.rationale(),
                proposal.status().name(),
                proposal.decidedBy(),
                proposal.decidedAt(),
                proposal.errorCode(),
                proposal.createdAt(),
                proposal.updatedAt());
        return findById(proposal.id()).orElseThrow();
    }

    @Override
    public Optional<AiTaskResolutionActionProposal> findById(UUID id) {
        return jdbc
                .query(
                        "SELECT * FROM ai_task_resolution_action_proposals WHERE id = ?",
                        this::map,
                        id)
                .stream()
                .findFirst();
    }

    @Override
    public List<AiTaskResolutionActionProposal> findByJobId(UUID jobId) {
        return jdbc.query(
                "SELECT * FROM ai_task_resolution_action_proposals WHERE job_id = ? ORDER BY created_at",
                this::map,
                jobId);
    }

    private AiTaskResolutionActionProposal map(ResultSet rs, int row) throws SQLException {
        return new AiTaskResolutionActionProposal(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("job_id"),
                rs.getString("proposed_action_type"),
                rs.getString("payload_preview"),
                rs.getString("risk_level"),
                rs.getString("rationale"),
                AiTaskResolutionActionProposalStatus.valueOf(rs.getString("status")),
                (UUID) rs.getObject("decided_by"),
                instant(rs, "decided_at"),
                rs.getString("error_code"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        java.sql.Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toInstant();
    }
}
