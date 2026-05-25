package com.taskmind.backend.task.infrastructure.persistence.jpa;

import com.taskmind.backend.task.domain.model.TaskStatus;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataTaskJpaRepository extends JpaRepository<TaskJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TaskJpaEntity t where t.id = :id")
    Optional<TaskJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
        select t
        from TaskJpaEntity t
        where (:userId is null or t.userId = :userId)
          and (:status is null or t.status = :status)
          and (:overdueOnly = false or (t.dueAt is not null and t.dueAt < :now and t.status not in (:doneStatus, :archivedStatus)))
        order by t.createdAt desc
        """)
    List<TaskJpaEntity> findFiltered(
        @Param("userId") UUID userId,
        @Param("status") TaskStatus status,
        @Param("overdueOnly") boolean overdueOnly,
        @Param("now") OffsetDateTime now,
        @Param("doneStatus") TaskStatus doneStatus,
        @Param("archivedStatus") TaskStatus archivedStatus,
        Pageable pageable
    );
}
