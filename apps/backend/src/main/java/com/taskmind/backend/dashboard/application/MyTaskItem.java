package com.taskmind.backend.dashboard.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MyTaskItem(
        UUID taskId, UUID projectId, String title, String status, OffsetDateTime updatedAt) {}
