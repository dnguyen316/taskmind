package com.taskmind.backend.task.infrastructure.persistence.jpa;

import com.taskmind.backend.task.domain.model.TaskLink;
import com.taskmind.backend.task.domain.repository.TaskLinkRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    public List<TaskLink> findForTask(UUID taskId) {
        return repository.findBySourceTaskIdOrTargetTaskId(taskId, taskId).stream()
                .map(TaskLinkJpaEntity::toDomain)
                .toList();
    }

    public void deleteById(UUID linkId) {
        repository.deleteById(linkId);
    }

    public Optional<TaskLink> findById(UUID linkId) {
        return repository.findById(linkId).map(TaskLinkJpaEntity::toDomain);
    }
}
