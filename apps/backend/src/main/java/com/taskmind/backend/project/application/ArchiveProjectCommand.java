package com.taskmind.backend.project.application;

import java.util.UUID;

public record ArchiveProjectCommand(
    UUID projectId
) {
}
