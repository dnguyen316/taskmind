package com.taskmind.backend.task.infrastructure.persistence.jpa;
public interface TaskReleaseStatsProjection {
    String getReleaseVersion();
    long getTotalTasks();
    long getCompletedTasks();
    Integer getTotalStoryPoints();
    Integer getCompletedStoryPoints();
}
