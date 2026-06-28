package com.taskmind.backend.tasktype.infrastructure.persistence.jpa;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataTaskTypeJpaRepository extends JpaRepository<TaskTypeJpaEntity, UUID> {
    @Query("select t from TaskTypeJpaEntity t where t.active=true and (t.projectId is null or t.projectId=:projectId) order by t.sortOrder, t.name")
    List<TaskTypeJpaEntity> findActive(@Param("projectId") UUID projectId);

    @Query("select t from TaskTypeJpaEntity t where t.active=true and t.key=:key and (t.projectId=:projectId or t.projectId is null) order by case when t.projectId is null then 1 else 0 end")
    List<TaskTypeJpaEntity> findActiveByKey(@Param("projectId") UUID projectId, @Param("key") String key);
}
