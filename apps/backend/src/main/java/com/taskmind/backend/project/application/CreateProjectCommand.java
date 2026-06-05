package com.taskmind.backend.project.application;

import java.util.UUID;

public record CreateProjectCommand(
    String name,
    String key,
    String description,
    UUID ownerUserId
) {
}
