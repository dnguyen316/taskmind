package com.taskmind.backend.specbreakdown.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownTemplate;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Sql(statements = "CREATE TABLE IF NOT EXISTS spec_breakdown_templates (id UUID PRIMARY KEY, project_id UUID NOT NULL, name VARCHAR(160) NOT NULL, description TEXT, fields TEXT NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, version BIGINT NOT NULL DEFAULT 0)")
@Import(JpaSpecBreakdownTemplateRepository.class)
class JpaSpecBreakdownTemplateRepositoryTest {
    @Autowired JpaSpecBreakdownTemplateRepository repository;

    @Test
    void persistsTemplatesAcrossRepositoryCalls() {
        UUID id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID projectId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        SpecBreakdownTemplate saved = repository.save(new SpecBreakdownTemplate(
                id, null, projectId, "Default breakdown", "Default", "{\"issueType\":\"Story\"}", now, now));

        assertThat(saved.version()).isNotNull();
        assertThat(repository.findById(id)).contains(saved);
        assertThat(repository.findByProjectId(projectId)).containsExactly(saved);

        SpecBreakdownTemplate updated = repository.save(new SpecBreakdownTemplate(
                saved.id(),
                saved.version(),
                saved.projectId(),
                "Updated breakdown",
                "Updated",
                "{\"issueType\":\"Task\"}",
                saved.createdAt(),
                now.plusSeconds(60)));

        assertThat(repository.findById(id)).contains(updated);
        assertThat(repository.findByProjectId(projectId)).containsExactly(updated);

        repository.delete(updated);

        assertThat(repository.findById(id)).isEmpty();
        assertThat(repository.findByProjectId(projectId)).isEmpty();
    }
}
