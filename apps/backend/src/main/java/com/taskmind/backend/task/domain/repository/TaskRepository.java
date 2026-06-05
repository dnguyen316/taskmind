package com.taskmind.backend.task.domain.repository;

import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {
    Task save(Task task);

    Optional<Task> findById(UUID id);

    Optional<Task> findByIdForUpdate(UUID id);

    List<Task> findAll();

    List<Task> findFiltered(
            Optional<UUID> userId,
            Optional<TaskStatus> status,
            boolean overdueOnly,
            OffsetDateTime now,
            int page,
            int size);
}
