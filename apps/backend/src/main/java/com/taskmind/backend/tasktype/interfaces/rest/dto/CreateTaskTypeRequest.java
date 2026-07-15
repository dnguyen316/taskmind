package com.taskmind.backend.tasktype.interfaces.rest.dto;

import java.util.UUID;

public record CreateTaskTypeRequest(UUID projectId, String key, String name, String color, String icon, Integer sortOrder) {}
