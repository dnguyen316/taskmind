package com.taskmind.backend.task.domain.repository;

import com.taskmind.backend.task.domain.model.TaskLink;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskLinkRepository {
    TaskLink save(TaskLink link);

    List<TaskLink> findForTask(UUID taskId);

    void deleteById(UUID linkId);

    Optional<TaskLink> findById(UUID linkId);
}
