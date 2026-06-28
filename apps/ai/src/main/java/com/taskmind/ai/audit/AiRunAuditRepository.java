package com.taskmind.ai.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import com.taskmind.ai.contracts.AiProviderId;
import com.taskmind.ai.contracts.AiRunStatus;
import com.taskmind.ai.contracts.audit.AiRunSummary;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AiRunAuditRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AiRunAuditRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public UUID start(AiRunRecord record) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                INSERT INTO ai.ai_runs (
                  id, user_id, workspace_id, capability_id, provider_id, model_id, status, request_hash,
                  input_json, prompt_version, validation_outcome, correlation_id, created_at, started_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """,
                ps -> {
                    ps.setObject(1, id);
                    ps.setObject(2, record.userId());
                    ps.setString(3, record.workspaceId());
                    ps.setString(4, record.capabilityId().value());
                    ps.setString(5, record.providerId().value());
                    ps.setString(6, record.modelId());
                    ps.setString(7, AiRunStatus.RUNNING.name());
                    ps.setString(8, record.requestHash());
                    ps.setString(9, toJson(record.input()));
                    ps.setString(10, record.promptVersion());
                    ps.setString(11, record.validationOutcome());
                    ps.setString(12, record.correlationId());
                    setInstant(ps, 13, now);
                    setInstant(ps, 14, now);
                });
        return id;
    }

    public void succeed(
            UUID runId,
            JsonNode output,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            long latencyMs) {
        jdbcTemplate.update(
                """
                UPDATE ai.ai_runs
                SET status = ?, output_json = ?, prompt_tokens = ?, completion_tokens = ?, total_tokens = ?,
                    latency_ms = ?, completed_at = ?, version = version + 1
                WHERE id = ?
                """,
                ps -> {
                    ps.setString(1, AiRunStatus.SUCCEEDED.name());
                    ps.setString(2, toJson(output));
                    ps.setInt(3, promptTokens);
                    ps.setInt(4, completionTokens);
                    ps.setInt(5, totalTokens);
                    ps.setLong(6, latencyMs);
                    setInstant(ps, 7, Instant.now());
                    ps.setObject(8, runId);
                });
    }

    public void fail(UUID runId, String errorCode, String errorMessage, long latencyMs) {
        jdbcTemplate.update(
                """
                UPDATE ai.ai_runs
                SET status = ?, error_code = ?, error_message = ?, latency_ms = ?, completed_at = ?, version = version + 1
                WHERE id = ?
                """,
                ps -> {
                    ps.setString(1, AiRunStatus.FAILED.name());
                    ps.setString(2, errorCode);
                    ps.setString(3, errorMessage);
                    ps.setLong(4, latencyMs);
                    setInstant(ps, 5, Instant.now());
                    ps.setObject(6, runId);
                });
    }

    public Optional<AiRunSummary> findSummary(UUID runId) {
        return jdbcTemplate
                .query(
                        """
                SELECT id, status, provider_id, capability_id, model_id, correlation_id, created_at, started_at, completed_at
                FROM ai.ai_runs WHERE id = ?
                """,
                        this::mapSummary,
                        runId)
                .stream()
                .findFirst();
    }

    private AiRunSummary mapSummary(ResultSet rs, int rowNum) throws SQLException {
        return new AiRunSummary(
                rs.getObject("id", UUID.class),
                AiRunStatus.valueOf(rs.getString("status")),
                new AiProviderId(rs.getString("provider_id")),
                new AiCapabilityId(rs.getString("capability_id")),
                rs.getString("model_id"),
                rs.getString("correlation_id"),
                rs.getObject("created_at", Instant.class),
                rs.getObject("started_at", Instant.class),
                rs.getObject("completed_at", Instant.class));
    }

    private void setInstant(PreparedStatement ps, int parameterIndex, Instant instant)
            throws SQLException {
        ps.setObject(parameterIndex, instant, Types.TIMESTAMP_WITH_TIMEZONE);
    }

    private String toJson(JsonNode node) {
        try {
            return node == null ? null : objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to serialize AI audit JSON", ex);
        }
    }
}
