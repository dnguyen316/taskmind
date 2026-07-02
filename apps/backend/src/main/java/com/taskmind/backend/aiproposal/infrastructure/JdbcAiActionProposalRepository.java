package com.taskmind.backend.aiproposal.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.proposal.*;
import com.taskmind.backend.aiproposal.domain.AiActionProposalRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAiActionProposalRepository implements AiActionProposalRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcAiActionProposalRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiActionProposalContract save(AiActionProposalContract p) {
        int updated =
                jdbc.update(
                        """
                UPDATE ai_action_proposals
                SET user_id = ?, action_type = ?, status = ?, proposed_payload = ?, preview = ?, rationale = ?,
                    proposer = ?, provider = ?, model = ?, source = ?, source_context = ?, created_at = ?,
                    expires_at = ?, accepted_at = ?, decided_by = ?, user_decision = ?
                WHERE id = ?
                """,
                        p.userId(),
                        p.actionType().name(),
                        p.status().name(),
                        p.proposedPayload().toString(),
                        p.preview(),
                        p.rationale(),
                        p.proposer(),
                        p.provider(),
                        p.model(),
                        p.source().name(),
                        p.sourceContext(),
                        p.createdAt(),
                        p.expiresAt(),
                        p.acceptedAt(),
                        p.decidedBy(),
                        p.userDecision(),
                        p.id());
        if (updated == 0) {
            jdbc.update(
                    """
                    INSERT INTO ai_action_proposals (id, user_id, action_type, status, proposed_payload, preview, rationale, proposer, provider, model, source, source_context, created_at, expires_at, accepted_at, decided_by, user_decision)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    p.id(),
                    p.userId(),
                    p.actionType().name(),
                    p.status().name(),
                    p.proposedPayload().toString(),
                    p.preview(),
                    p.rationale(),
                    p.proposer(),
                    p.provider(),
                    p.model(),
                    p.source().name(),
                    p.sourceContext(),
                    p.createdAt(),
                    p.expiresAt(),
                    p.acceptedAt(),
                    p.decidedBy(),
                    p.userDecision());
        }
        return p;
    }

    @Override
    public Optional<AiActionProposalContract> findById(UUID id) {
        return jdbc.query("SELECT * FROM ai_action_proposals WHERE id = ?", this::map, id).stream()
                .findFirst();
    }

    @Override
    public List<AiActionProposalContract> findByUserIdAndStatus(
            UUID userId, AiActionProposalStatus status) {
        return jdbc.query(
                "SELECT * FROM ai_action_proposals WHERE user_id = ? AND status = ? ORDER BY created_at DESC",
                this::map,
                userId,
                status.name());
    }

    private AiActionProposalContract map(ResultSet rs, int row) throws SQLException {
        return new AiActionProposalContract(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("user_id"),
                AiActionProposalType.valueOf(rs.getString("action_type")),
                AiActionProposalStatus.valueOf(rs.getString("status")),
                readJson(rs.getString("proposed_payload")),
                rs.getString("preview"),
                rs.getString("rationale"),
                rs.getString("proposer"),
                rs.getString("provider"),
                rs.getString("model"),
                AiActionProposalSource.valueOf(rs.getString("source")),
                rs.getString("source_context"),
                rs.getObject("created_at", Instant.class),
                rs.getObject("expires_at", Instant.class),
                rs.getObject("accepted_at", Instant.class),
                (UUID) rs.getObject("decided_by"),
                rs.getString("user_decision"));
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json == null ? "{}" : json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid proposal JSON", e);
        }
    }
}
