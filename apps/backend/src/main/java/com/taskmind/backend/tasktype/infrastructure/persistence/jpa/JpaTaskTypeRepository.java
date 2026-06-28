package com.taskmind.backend.tasktype.infrastructure.persistence.jpa;

import com.taskmind.backend.tasktype.domain.model.TaskTypeDefinition;
import com.taskmind.backend.tasktype.domain.repository.TaskTypeRepository;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTaskTypeRepository implements TaskTypeRepository {
    private final SpringDataTaskTypeJpaRepository repo;
    public JpaTaskTypeRepository(SpringDataTaskTypeJpaRepository repo) { this.repo = repo; }
    public TaskTypeDefinition save(TaskTypeDefinition taskType) { return repo.save(TaskTypeJpaEntity.fromDomain(taskType)).toDomain(); }
    public List<TaskTypeDefinition> findActive(UUID projectId) { return repo.findActive(projectId).stream().map(TaskTypeJpaEntity::toDomain).toList(); }
    public Optional<TaskTypeDefinition> findById(UUID id) { return repo.findById(id).map(TaskTypeJpaEntity::toDomain); }
    public Optional<TaskTypeDefinition> findActiveByKey(UUID projectId, String key) { return repo.findActiveByKey(projectId, key).stream().findFirst().map(TaskTypeJpaEntity::toDomain); }
}
