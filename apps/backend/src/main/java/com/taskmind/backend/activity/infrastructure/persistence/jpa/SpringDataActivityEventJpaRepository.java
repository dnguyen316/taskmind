package com.taskmind.backend.activity.infrastructure.persistence.jpa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataActivityEventJpaRepository
        extends JpaRepository<ActivityEventJpaEntity, UUID> {}
