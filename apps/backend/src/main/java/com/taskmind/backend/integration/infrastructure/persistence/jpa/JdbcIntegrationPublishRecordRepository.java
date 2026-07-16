package com.taskmind.backend.integration.infrastructure.persistence.jpa;

import com.taskmind.backend.integration.domain.model.IntegrationProvider;
import com.taskmind.backend.integration.domain.model.IntegrationPublishRecord;
import com.taskmind.backend.integration.domain.repository.IntegrationPublishRecordRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcIntegrationPublishRecordRepository implements IntegrationPublishRecordRepository {

    private final JdbcTemplate jdbc;

    public JdbcIntegrationPublishRecordRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public IntegrationPublishRecord save(IntegrationPublishRecord record) {
        jdbc.update(
                "MERGE INTO integration_publish_records (id,version,task_id,project_link_id,provider,external_id,external_key,external_url,status,published_by,published_at,metadata_json) KEY(id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                record.id(),
                0,
                record.taskId(),
                record.projectLinkId(),
                record.provider().name(),
                record.externalId(),
                record.externalKey(),
                record.externalUrl(),
                record.status(),
                record.publishedBy(),
                record.publishedAt(),
                record.metadataJson());
        return record;
    }

    public Optional<IntegrationPublishRecord> findByTaskIdAndProjectLinkId(UUID taskId, UUID projectLinkId) {
        return jdbc.query(
                        "SELECT * FROM integration_publish_records WHERE task_id=? AND project_link_id=? ORDER BY published_at LIMIT 1",
                        this::map,
                        taskId,
                        projectLinkId)
                .stream()
                .findFirst();
    }

    private IntegrationPublishRecord map(ResultSet resultSet, int row) throws SQLException {
        return new IntegrationPublishRecord(
                (UUID) resultSet.getObject("id"),
                resultSet.getLong("version"),
                (UUID) resultSet.getObject("task_id"),
                (UUID) resultSet.getObject("project_link_id"),
                IntegrationProvider.valueOf(resultSet.getString("provider")),
                resultSet.getString("external_id"),
                resultSet.getString("external_key"),
                resultSet.getString("external_url"),
                resultSet.getString("status"),
                (UUID) resultSet.getObject("published_by"),
                timestampToInstant(resultSet, "published_at"),
                resultSet.getString("metadata_json"));
    }

    private Instant timestampToInstant(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
