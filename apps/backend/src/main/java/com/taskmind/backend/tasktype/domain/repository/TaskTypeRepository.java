package com.taskmind.backend.tasktype.domain.repository;

import com.taskmind.backend.tasktype.domain.model.TaskTypeDefinition;
import java.util.*;

public interface TaskTypeRepository {
    TaskTypeDefinition save(TaskTypeDefinition taskType);
    List<TaskTypeDefinition> findActive(UUID projectId);
    Optional<TaskTypeDefinition> findById(UUID id);
    Optional<TaskTypeDefinition> findActiveByKey(UUID projectId, String key);
}
