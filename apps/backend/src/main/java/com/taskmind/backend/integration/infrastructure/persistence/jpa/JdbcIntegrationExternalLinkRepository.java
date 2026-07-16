package com.taskmind.backend.integration.infrastructure.persistence.jpa;

import com.taskmind.backend.integration.domain.model.IntegrationExternalLink;
import com.taskmind.backend.integration.domain.model.IntegrationProvider;
import com.taskmind.backend.integration.domain.repository.IntegrationExternalLinkRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcIntegrationExternalLinkRepository implements IntegrationExternalLinkRepository {

    private final JdbcTemplate jdbc;

    public JdbcIntegrationExternalLinkRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public IntegrationExternalLink save(IntegrationExternalLink link) {
        jdbc.update(
                "MERGE INTO integration_external_links (id,version,task_id,project_id,provider,external_type,external_id,external_key,external_url,direction,metadata_json,repository_owner,repository_name,external_number,git_sha,check_run_id,created_at,updated_at) KEY(id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                link.id(),
                0,
                link.taskId(),
                link.projectId(),
                link.provider().name(),
                link.externalType(),
                link.externalId(),
                link.externalKey(),
                link.externalUrl(),
                link.direction(),
                link.metadataJson(),
                link.repositoryOwner(),
                link.repositoryName(),
                link.externalNumber(),
                link.gitSha(),
                link.checkRunId(),
                link.createdAt(),
                link.updatedAt());
        return link;
    }

    public Optional<IntegrationExternalLink> findByTaskIdAndProvider(UUID taskId, IntegrationProvider provider) {
        return jdbc.query(
                        "SELECT * FROM integration_external_links WHERE task_id=? AND provider=? ORDER BY created_at LIMIT 1",
                        this::map,
                        taskId,
                        provider.name())
                .stream()
                .findFirst();
    }

    public Optional<IntegrationExternalLink> findByProviderAndExternalTypeAndExternalIdentity(
            IntegrationProvider provider, String externalType, String externalId, String externalKey) {
        String sql =
                "SELECT * FROM integration_external_links WHERE provider=? AND external_type=? AND (external_id=? OR (? IS NOT NULL AND external_key=?)) ORDER BY created_at LIMIT 1";
        return jdbc.query(sql, this::map, provider.name(), externalType, externalId, externalKey, externalKey)
                .stream()
                .findFirst();
    }

    private IntegrationExternalLink map(ResultSet resultSet, int row) throws SQLException {
        return new IntegrationExternalLink(
                (UUID) resultSet.getObject("id"),
                resultSet.getLong("version"),
                (UUID) resultSet.getObject("task_id"),
                (UUID) resultSet.getObject("project_id"),
                IntegrationProvider.valueOf(resultSet.getString("provider")),
                resultSet.getString("external_type"),
                resultSet.getString("external_id"),
                resultSet.getString("external_key"),
                resultSet.getString("external_url"),
                resultSet.getString("direction"),
                resultSet.getString("metadata_json"),
                resultSet.getString("repository_owner"),
                resultSet.getString("repository_name"),
                (Integer) resultSet.getObject("external_number"),
                resultSet.getString("git_sha"),
                resultSet.getString("check_run_id"),
                timestampToInstant(resultSet, "created_at"),
                timestampToInstant(resultSet, "updated_at"));
    }

    private Instant timestampToInstant(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
