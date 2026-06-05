package com.taskmind.backend.project.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.UUID;

public record Project(
    UUID id,
    @JsonIgnore
    Long version,
    String name,
    String key,
    String description,
    UUID ownerUserId,
    Instant archivedAt,
    Instant createdAt,
    Instant updatedAt
) {

    public Project withArchivedAt(Instant archivedAtTime, Instant updatedAtTime) {
        return new Project(id, version, name, key, description, ownerUserId, archivedAtTime, createdAt, updatedAtTime);
    }
}
