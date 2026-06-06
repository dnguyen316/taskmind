package com.taskmind.backend.attachment.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataTaskAttachmentJpaRepository
        extends JpaRepository<TaskAttachmentJpaEntity, UUID> {
    @Query("select a from TaskAttachmentJpaEntity a where a.id=:id and a.deletedAt is null")
    Optional<TaskAttachmentJpaEntity> findActiveById(@Param("id") UUID id);

    @Query(
            "select a from TaskAttachmentJpaEntity a where a.taskId=:taskId and a.deletedAt is null order by a.createdAt desc")
    List<TaskAttachmentJpaEntity> findActiveByTaskId(@Param("taskId") UUID taskId);
}
