package com.taskmind.backend.specbreakdown.infrastructure.persistence.jpa;

import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownTemplate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spec_breakdown_templates")
public class SpecBreakdownTemplateJpaEntity {
    @Id UUID id;
    @Version Long version;
    UUID projectId;
    String name;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(columnDefinition = "TEXT")
    String fields;

    Instant createdAt;
    Instant updatedAt;

    protected SpecBreakdownTemplateJpaEntity() {}

    static SpecBreakdownTemplateJpaEntity from(SpecBreakdownTemplate template) {
        SpecBreakdownTemplateJpaEntity entity = new SpecBreakdownTemplateJpaEntity();
        entity.id = template.id();
        entity.version = template.version();
        entity.projectId = template.projectId();
        entity.name = template.name();
        entity.description = template.description();
        entity.fields = template.fields();
        entity.createdAt = template.createdAt();
        entity.updatedAt = template.updatedAt();
        return entity;
    }

    SpecBreakdownTemplate toDomain() {
        return new SpecBreakdownTemplate(id, version, projectId, name, description, fields, createdAt, updatedAt);
    }
}
