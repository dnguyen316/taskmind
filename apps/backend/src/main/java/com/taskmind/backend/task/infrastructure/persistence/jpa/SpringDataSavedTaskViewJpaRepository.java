package com.taskmind.backend.task.infrastructure.persistence.jpa;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSavedTaskViewJpaRepository extends JpaRepository<SavedTaskViewJpaEntity, UUID> {
    List<SavedTaskViewJpaEntity> findByUserIdOrderByCreatedAtAsc(UUID userId);
    Optional<SavedTaskViewJpaEntity> findByIdAndUserId(UUID id, UUID userId);
}
