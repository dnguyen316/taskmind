package com.taskmind.backend.task.infrastructure.persistence;

import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.repository.TaskRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTaskRepository implements TaskRepository {

    private final ConcurrentMap<UUID, Task> tasksById = new ConcurrentHashMap<>();

    @Override
    public Task save(Task task) {
        tasksById.put(task.id(), task);
        return task;
    }

    @Override
    public Optional<Task> findById(UUID id) {
        return Optional.ofNullable(tasksById.get(id));
    }

    @Override
    public List<Task> findAll() {
        return new ArrayList<>(tasksById.values());
    }
}
