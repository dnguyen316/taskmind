package com.taskmind.backend.project.infrastructure.persistence.jpa;

import com.taskmind.backend.project.domain.model.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class ProjectJpaEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "key", nullable = false, length = 20, unique = true)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProjectJpaEntity() {
    }

    private ProjectJpaEntity(Project project) {
        this.id = project.id();
        this.name = project.name();
        this.key = project.key();
        this.description = project.description();
        this.ownerUserId = project.ownerUserId();
        this.archivedAt = project.archivedAt();
        this.createdAt = project.createdAt();
        this.updatedAt = project.updatedAt();
    }

    public static ProjectJpaEntity fromDomain(Project project) {
        return new ProjectJpaEntity(project);
    }

    public Project toDomain() {
        return new Project(
            id,
            name,
            key,
            description,
            ownerUserId,
            archivedAt,
            createdAt,
            updatedAt
        );
    }
}
