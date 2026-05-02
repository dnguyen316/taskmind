package com.taskmind.backend.task.infrastructure.persistence.jpa;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataTaskJpaRepository extends JpaRepository<TaskJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TaskJpaEntity t where t.id = :id")
    Optional<TaskJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
