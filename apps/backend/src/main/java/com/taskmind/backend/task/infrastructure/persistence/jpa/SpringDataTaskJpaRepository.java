package com.taskmind.backend.task.infrastructure.persistence.jpa;

import com.taskmind.backend.task.domain.model.TaskStatus;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SpringDataTaskJpaRepository extends JpaRepository<TaskJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select t from TaskJpaEntity t where t.id=:id and t.deletedAt is null") Optional<TaskJpaEntity> findByIdForUpdate(@Param("id") UUID id);
    @Query("select t from TaskJpaEntity t where t.id=:id and t.deletedAt is null") Optional<TaskJpaEntity> findActiveById(@Param("id") UUID id);
    @Query("select t from TaskJpaEntity t where t.parentTaskId=:parentId and t.deletedAt is null order by t.createdAt") List<TaskJpaEntity> findChildren(@Param("parentId") UUID parentId);
    @Query("select t from TaskJpaEntity t where (:userId is null or t.userId=:userId) and (:status is null or t.status=:status) and t.deletedAt is null and (:overdueOnly=false or (t.dueAt is not null and t.dueAt<:now and t.status not in (:doneStatus,:archivedStatus))) order by t.createdAt desc")
    List<TaskJpaEntity> findFiltered(@Param("userId") UUID userId,@Param("status") TaskStatus status,@Param("overdueOnly") boolean overdueOnly,@Param("now") OffsetDateTime now,@Param("doneStatus") TaskStatus doneStatus,@Param("archivedStatus") TaskStatus archivedStatus,Pageable pageable);

    @Query("""
            select t from TaskJpaEntity t
            where (:userId is null or t.userId=:userId)
              and (:status is null or t.status=:status)
              and (:priority is null or t.priority=:priority)
              and (:projectId is null or t.projectId=:projectId)
              and (:assigneeId is null or t.assigneeId=:assigneeId)
              and (:archived=true or t.status<>:archivedStatus)
              and (:archived=false or t.status=:archivedStatus)
              and (:dueToday=false or (t.dueAt is not null and t.dueAt>=:todayStart and t.dueAt<:tomorrowStart))
              and (:overdue=false or (t.dueAt is not null and t.dueAt<:now and t.status not in (:doneStatus,:archivedStatus)))
              and (:unassigned=false or t.assigneeId is null)
              and (:noDueDate=false or t.dueAt is null)
              and (:stale=false or (t.updatedAt<:staleBefore and t.status not in (:doneStatus,:archivedStatus)))
              and (:blocked=false or exists (select l.id from TaskLinkJpaEntity l where l.targetTaskId=t.id and l.linkType=com.taskmind.backend.task.domain.model.TaskLinkType.BLOCKS))
              and t.deletedAt is null
            order by
              case when :sort='dueAt' then t.dueAt end asc,
              case when :sort='priority' then t.priority end asc,
              case when :sort='createdAt' then t.createdAt end desc,
              t.updatedAt desc
            """)
    List<TaskJpaEntity> findRichFiltered(
            @Param("userId") UUID userId,
            @Param("status") TaskStatus status,
            @Param("priority") Integer priority,
            @Param("projectId") UUID projectId,
            @Param("assigneeId") UUID assigneeId,
            @Param("dueToday") boolean dueToday,
            @Param("overdue") boolean overdue,
            @Param("blocked") boolean blocked,
            @Param("unassigned") boolean unassigned,
            @Param("noDueDate") boolean noDueDate,
            @Param("stale") boolean stale,
            @Param("archived") boolean archived,
            @Param("now") OffsetDateTime now,
            @Param("todayStart") OffsetDateTime todayStart,
            @Param("tomorrowStart") OffsetDateTime tomorrowStart,
            @Param("staleBefore") java.time.Instant staleBefore,
            @Param("doneStatus") TaskStatus doneStatus,
            @Param("archivedStatus") TaskStatus archivedStatus,
            @Param("sort") String sort,
            Pageable pageable);
    @Query(value="select release_version as releaseVersion, count(*) as totalTasks, sum(case when status='DONE' then 1 else 0 end) as completedTasks, coalesce(sum(story_points),0) as totalStoryPoints, coalesce(sum(case when status='DONE' then story_points else 0 end),0) as completedStoryPoints from tasks where project_id=:projectId and deleted_at is null and release_version is not null group by release_version order by release_version",nativeQuery=true)
    List<TaskReleaseStatsProjection> releaseStats(@Param("projectId") UUID projectId);
}
