package com.taskmind.backend.project.infrastructure.persistence.jpa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectJpaRepository extends JpaRepository<ProjectJpaEntity, UUID> {

    boolean existsByKeyIgnoreCase(String key);
}
