package com.taskmind.backend.integration.infrastructure.persistence.jpa;

import com.taskmind.backend.integration.domain.model.IntegrationConnection;
import com.taskmind.backend.integration.domain.model.IntegrationProvider;
import com.taskmind.backend.integration.domain.repository.IntegrationConnectionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcIntegrationConnectionRepository implements IntegrationConnectionRepository {

    private final JdbcTemplate jdbc;

    public JdbcIntegrationConnectionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public IntegrationConnection save(IntegrationConnection connection) {
        jdbc.update(
                "MERGE INTO integration_connections (id,version,provider,account_name,base_url,account_external_id,owner_user_id,encrypted_access_token,encrypted_refresh_token,token_expires_at,scopes,status,created_at,updated_at) KEY(id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                connection.id(),
                0,
                connection.provider().name(),
                connection.accountName(),
                connection.baseUrl(),
                connection.accountExternalId(),
                connection.ownerUserId(),
                connection.encryptedAccessToken(),
                connection.encryptedRefreshToken(),
                connection.tokenExpiresAt(),
                connection.scopes(),
                connection.status(),
                connection.createdAt(),
                connection.updatedAt());
        return connection;
    }

    public List<IntegrationConnection> findByOwnerUserId(UUID ownerUserId) {
        return jdbc.query(
                "SELECT * FROM integration_connections WHERE owner_user_id=? ORDER BY created_at",
                this::map,
                ownerUserId);
    }

    public Optional<IntegrationConnection> findById(UUID id) {
        return jdbc.query("SELECT * FROM integration_connections WHERE id=?", this::map, id).stream().findFirst();
    }

    private IntegrationConnection map(ResultSet resultSet, int row) throws SQLException {
        return new IntegrationConnection(
                (UUID) resultSet.getObject("id"),
                resultSet.getLong("version"),
                IntegrationProvider.valueOf(resultSet.getString("provider")),
                resultSet.getString("account_name"),
                resultSet.getString("base_url"),
                resultSet.getString("account_external_id"),
                (UUID) resultSet.getObject("owner_user_id"),
                resultSet.getString("encrypted_access_token"),
                resultSet.getString("encrypted_refresh_token"),
                timestampToInstant(resultSet, "token_expires_at"),
                resultSet.getString("scopes"),
                resultSet.getString("status"),
                timestampToInstant(resultSet, "created_at"),
                timestampToInstant(resultSet, "updated_at"));
    }

    private Instant timestampToInstant(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
