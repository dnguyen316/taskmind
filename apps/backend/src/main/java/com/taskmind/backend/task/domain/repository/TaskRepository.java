package com.taskmind.backend.task.domain.repository;

import com.taskmind.backend.task.domain.model.Task;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {
    Task save(Task task);

    Optional<Task> findById(UUID id);

    List<Task> findAll();
}
