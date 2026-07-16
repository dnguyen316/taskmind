package com.taskmind.backend.integration.infrastructure.persistence.jpa;

import com.taskmind.backend.integration.domain.model.IntegrationImportRun;
import com.taskmind.backend.integration.domain.repository.IntegrationImportRunRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcIntegrationImportRunRepository implements IntegrationImportRunRepository {

    private final JdbcTemplate jdbc;

    public JdbcIntegrationImportRunRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public IntegrationImportRun save(IntegrationImportRun run) {
        jdbc.update(
                "MERGE INTO integration_import_runs (id,version,project_id,project_link_id,provider,status,imported_count,skipped_count,error_message,requested_by,created_at,completed_at) KEY(id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                run.id(),
                0,
                run.projectId(),
                run.projectLinkId(),
                run.provider().name(),
                run.status(),
                run.importedCount(),
                run.skippedCount(),
                run.errorMessage(),
                run.requestedBy(),
                run.createdAt(),
                run.completedAt());
        return run;
    }
}
