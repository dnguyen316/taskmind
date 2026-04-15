package com.taskmind.backend.task.infrastructure.persistence.jpa;

import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.repository.TaskRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTaskRepository implements TaskRepository {

    private final SpringDataTaskJpaRepository taskJpaRepository;

    public JpaTaskRepository(SpringDataTaskJpaRepository taskJpaRepository) {
        this.taskJpaRepository = taskJpaRepository;
    }

    @Override
    public Task save(Task task) {
        var persisted = taskJpaRepository.save(TaskJpaEntity.fromDomain(task));
        return persisted.toDomain();
    }

    @Override
    public Optional<Task> findById(UUID id) {
        return taskJpaRepository.findById(id).map(TaskJpaEntity::toDomain);
    }

    @Override
    public List<Task> findAll() {
        return taskJpaRepository.findAll().stream().map(TaskJpaEntity::toDomain).toList();
    }
}
