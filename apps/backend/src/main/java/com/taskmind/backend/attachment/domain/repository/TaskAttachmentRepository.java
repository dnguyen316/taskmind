package com.taskmind.backend.attachment.domain.repository;

import com.taskmind.backend.attachment.domain.model.TaskAttachment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskAttachmentRepository {
    TaskAttachment save(TaskAttachment attachment);

    Optional<TaskAttachment> findActiveById(UUID id);

    List<TaskAttachment> findActiveByTaskId(UUID taskId);
}
