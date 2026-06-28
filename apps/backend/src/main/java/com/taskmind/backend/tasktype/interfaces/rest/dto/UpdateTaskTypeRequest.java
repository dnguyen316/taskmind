package com.taskmind.backend.tasktype.interfaces.rest.dto;

public record UpdateTaskTypeRequest(String name, String color, String icon, Boolean active, Integer sortOrder) {}
