package com.taskmind.backend.integration.infrastructure.persistence.jpa;

import com.taskmind.backend.integration.domain.model.*;
import com.taskmind.backend.integration.domain.repository.IntegrationProjectLinkRepository;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcIntegrationProjectLinkRepository implements IntegrationProjectLinkRepository {
    private final JdbcTemplate jdbc;
    public JdbcIntegrationProjectLinkRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public IntegrationProjectLink save(IntegrationProjectLink l) {
        jdbc.update("MERGE INTO integration_project_links (id,version,project_id,connection_id,provider,external_project_id,external_project_key,external_project_name,metadata_json,repository_owner,repository_name,default_branch,installation_id,account_id,allowed_operations_json,created_by,created_at,updated_at) KEY(id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", l.id(), 0, l.projectId(), l.connectionId(), l.provider().name(), l.externalProjectId(), l.externalProjectKey(), l.externalProjectName(), l.metadataJson(), l.repositoryOwner(), l.repositoryName(), l.defaultBranch(), l.installationId(), l.accountId(), l.allowedOperationsJson(), l.createdBy(), l.createdAt(), l.updatedAt());
        return l;
    }
    public List<IntegrationProjectLink> findByProjectId(UUID projectId) { return jdbc.query("SELECT * FROM integration_project_links WHERE project_id=? ORDER BY created_at", this::map, projectId); }
    public Optional<IntegrationProjectLink> findById(UUID id) { return jdbc.query("SELECT * FROM integration_project_links WHERE id=?", this::map, id).stream().findFirst(); }
    private IntegrationProjectLink map(ResultSet rs, int row) throws SQLException { return new IntegrationProjectLink((UUID) rs.getObject("id"), rs.getLong("version"), (UUID) rs.getObject("project_id"), (UUID) rs.getObject("connection_id"), IntegrationProvider.valueOf(rs.getString("provider")), rs.getString("external_project_id"), rs.getString("external_project_key"), rs.getString("external_project_name"), rs.getString("metadata_json"), rs.getString("repository_owner"), rs.getString("repository_name"), rs.getString("default_branch"), rs.getString("installation_id"), rs.getString("account_id"), rs.getString("allowed_operations_json"), (UUID) rs.getObject("created_by"), i(rs, "created_at"), i(rs, "updated_at")); }
    private Instant i(ResultSet rs, String c) throws SQLException { Timestamp ts = rs.getTimestamp(c); return ts == null ? null : ts.toInstant(); }
}
