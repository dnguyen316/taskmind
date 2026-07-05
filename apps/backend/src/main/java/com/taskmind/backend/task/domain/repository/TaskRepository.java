package com.taskmind.backend.task.domain.repository;
import com.taskmind.backend.task.domain.model.*;
import com.taskmind.backend.task.infrastructure.persistence.jpa.TaskReleaseStatsProjection;
import com.taskmind.backend.task.application.TaskQuery;
import java.time.OffsetDateTime; import java.util.*;
public interface TaskRepository {
 Task save(Task task); Optional<Task> findById(UUID id); Optional<Task> findByIdForUpdate(UUID id); List<Task> findAll();
 List<Task> findChildren(UUID parentId); List<Task> findAncestors(UUID id); List<TaskReleaseStatsProjection> releaseStats(UUID projectId);
 List<Task> findFiltered(Optional<UUID> userId,Optional<TaskStatus> status,boolean overdueOnly,OffsetDateTime now,int page,int size);
 default List<Task> findFiltered(TaskQuery query, OffsetDateTime now, OffsetDateTime todayStart, OffsetDateTime tomorrowStart, java.time.Instant staleBefore) {
  return findFiltered(Optional.ofNullable(query.userId()), Optional.ofNullable(query.status()), Boolean.TRUE.equals(query.overdue()), now, query.page(), query.size());
 }
}
