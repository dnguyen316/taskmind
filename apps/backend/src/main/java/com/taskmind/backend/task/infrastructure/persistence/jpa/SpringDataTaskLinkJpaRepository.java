package com.taskmind.backend.task.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTaskLinkJpaRepository extends JpaRepository<TaskLinkJpaEntity, UUID> {
    List<TaskLinkJpaEntity> findBySourceTaskIdOrTargetTaskId(
            UUID sourceTaskId,
            UUID targetTaskId);
}
