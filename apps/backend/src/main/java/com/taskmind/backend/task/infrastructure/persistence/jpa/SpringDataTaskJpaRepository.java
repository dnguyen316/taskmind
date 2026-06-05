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
    @Query(value="select release_version as releaseVersion, count(*) as totalTasks, sum(case when status='DONE' then 1 else 0 end) as completedTasks, coalesce(sum(story_points),0) as totalStoryPoints, coalesce(sum(case when status='DONE' then story_points else 0 end),0) as completedStoryPoints from tasks where project_id=:projectId and deleted_at is null and release_version is not null group by release_version order by release_version",nativeQuery=true)
    List<TaskReleaseStatsProjection> releaseStats(@Param("projectId") UUID projectId);
}
