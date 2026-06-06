package com.taskmind.backend.attachment.application;

import com.taskmind.backend.attachment.config.AttachmentStorageProperties;
import com.taskmind.backend.attachment.domain.model.MediaKind;
import com.taskmind.backend.attachment.domain.model.TaskAttachment;
import com.taskmind.backend.attachment.domain.repository.ObjectStoragePort;
import com.taskmind.backend.attachment.domain.repository.TaskAttachmentRepository;
import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.task.application.TaskApplicationService;
import com.taskmind.backend.task.domain.model.Task;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskAttachmentApplicationService {
    private final TaskApplicationService tasks;
    private final TaskAttachmentRepository attachments;
    private final ObjectStoragePort storage;
    private final AttachmentStorageProperties properties;

    public TaskAttachmentApplicationService(
            TaskApplicationService tasks,
            TaskAttachmentRepository attachments,
            ObjectStoragePort storage,
            AttachmentStorageProperties properties) {
        this.tasks = tasks;
        this.attachments = attachments;
        this.storage = storage;
        this.properties = properties;
    }

    @Transactional
    public TaskAttachment upload(
            AuthenticatedUser requester,
            UUID taskId,
            String fileName,
            String contentType,
            long sizeBytes,
            MediaKind mediaKind,
            InputStream content) {
        Task task = requireTask(requester, taskId);
        validate(fileName, sizeBytes, mediaKind);
        UUID id = UUID.randomUUID();
        String key = "tasks/%s/attachments/%s/%s".formatted(taskId, id, sanitize(fileName));
        String normalizedContentType =
                contentType == null || contentType.isBlank()
                        ? "application/octet-stream"
                        : contentType;
        try {
            storage.put(key, content, sizeBytes, normalizedContentType);
        } catch (IOException e) {
            throw new IllegalStateException("Could not store attachment object", e);
        }
        Instant now = Instant.now();
        return attachments.save(
                new TaskAttachment(
                        id,
                        null,
                        taskId,
                        task.userId(),
                        key,
                        fileName,
                        normalizedContentType,
                        sizeBytes,
                        mediaKind,
                        null,
                        now,
                        now));
    }

    public List<TaskAttachment> list(AuthenticatedUser requester, UUID taskId) {
        requireTask(requester, taskId);
        return attachments.findActiveByTaskId(taskId);
    }

    public Download download(AuthenticatedUser requester, UUID taskId, UUID attachmentId) {
        requireTask(requester, taskId);
        TaskAttachment attachment = requireAttachment(taskId, attachmentId);
        try {
            ObjectStoragePort.StoredObject object = storage.get(attachment.objectKey());
            return new Download(attachment, object.bytes(), object.contentType());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read attachment object", e);
        }
    }

    @Transactional
    public void delete(AuthenticatedUser requester, UUID taskId, UUID attachmentId) {
        requireTask(requester, taskId);
        TaskAttachment attachment = requireAttachment(taskId, attachmentId);
        try {
            storage.delete(attachment.objectKey());
        } catch (IOException ignored) {
            // Metadata tombstone is still authoritative; storage cleanup can be retried
            // operationally.
        }
        Instant now = Instant.now();
        attachments.save(
                new TaskAttachment(
                        attachment.id(),
                        attachment.version(),
                        attachment.taskId(),
                        attachment.ownerUserId(),
                        attachment.objectKey(),
                        attachment.fileName(),
                        attachment.contentType(),
                        attachment.sizeBytes(),
                        attachment.mediaKind(),
                        now,
                        attachment.createdAt(),
                        now));
    }

    private Task requireTask(AuthenticatedUser requester, UUID taskId) {
        return tasks.findById(requester, taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found or access denied"));
    }

    private TaskAttachment requireAttachment(UUID taskId, UUID attachmentId) {
        TaskAttachment attachment =
                attachments
                        .findActiveById(attachmentId)
                        .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
        if (!attachment.taskId().equals(taskId)) {
            throw new IllegalArgumentException("Attachment not found");
        }
        return attachment;
    }

    private void validate(String fileName, long sizeBytes, MediaKind mediaKind) {
        if (fileName == null || fileName.isBlank())
            throw new IllegalArgumentException("File name is required");
        if (mediaKind == null) throw new IllegalArgumentException("Media kind is required");
        if (sizeBytes <= 0 || sizeBytes > properties.getMaxSizeBytes())
            throw new IllegalArgumentException("Attachment size exceeds configured limit");
    }

    private String sanitize(String fileName) {
        return fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    public record Download(TaskAttachment attachment, byte[] bytes, String contentType) {}
}
