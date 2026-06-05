package com.taskmind.backend.task.domain;
public final class TaskLeafSqlFilters {
    public static final String NOT_DELETED = "deleted_at IS NULL";
    private TaskLeafSqlFilters() {}
}
