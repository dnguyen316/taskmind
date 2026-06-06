package com.taskmind.backend.attachment.infrastructure.persistence.jpa;

import com.taskmind.backend.attachment.domain.model.TaskAttachment;
import com.taskmind.backend.attachment.domain.repository.TaskAttachmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTaskAttachmentRepository implements TaskAttachmentRepository {
    private final SpringDataTaskAttachmentJpaRepository repository;

    public JpaTaskAttachmentRepository(SpringDataTaskAttachmentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public TaskAttachment save(TaskAttachment attachment) {
        return repository.saveAndFlush(TaskAttachmentJpaEntity.fromDomain(attachment)).toDomain();
    }

    @Override
    public Optional<TaskAttachment> findActiveById(UUID id) {
        return repository.findActiveById(id).map(TaskAttachmentJpaEntity::toDomain);
    }

    @Override
    public List<TaskAttachment> findActiveByTaskId(UUID taskId) {
        return repository.findActiveByTaskId(taskId).stream()
                .map(TaskAttachmentJpaEntity::toDomain)
                .toList();
    }
}
