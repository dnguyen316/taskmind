package com.taskmind.backend.task.infrastructure.persistence.jpa;

import com.taskmind.backend.task.domain.model.TaskLink;
import com.taskmind.backend.task.domain.repository.TaskLinkRepository;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTaskLinkRepository implements TaskLinkRepository {
    private final SpringDataTaskLinkJpaRepository repository;

    public JpaTaskLinkRepository(SpringDataTaskLinkJpaRepository repository) {
        this.repository = repository;
    }

    public TaskLink save(TaskLink link) {
        return repository.save(TaskLinkJpaEntity.fromDomain(link)).toDomain();
    }

    public List<TaskLink> findForTask(UUID id) {
        return repository.findBySourceTaskIdOrTargetTaskId(id, id).stream()
                .map(TaskLinkJpaEntity::toDomain)
                .toList();
    }

    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    public Optional<TaskLink> findById(UUID id) {
        return repository.findById(id).map(TaskLinkJpaEntity::toDomain);
    }
}
