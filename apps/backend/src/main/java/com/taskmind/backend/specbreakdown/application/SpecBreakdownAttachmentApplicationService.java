package com.taskmind.backend.specbreakdown.application;

import com.taskmind.backend.attachment.config.AttachmentStorageProperties;
import com.taskmind.backend.attachment.domain.repository.ObjectStoragePort;
import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownAttachment;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownDraft;
import com.taskmind.backend.specbreakdown.domain.repository.SpecBreakdownAttachmentRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SpecBreakdownAttachmentApplicationService {
    private final SpecBreakdownApplicationService drafts;
    private final SpecBreakdownAttachmentRepository attachments;
    private final ObjectStoragePort storage;
    private final AttachmentStorageProperties properties;

    public SpecBreakdownAttachmentApplicationService(
            SpecBreakdownApplicationService drafts,
            SpecBreakdownAttachmentRepository attachments,
            ObjectStoragePort storage,
            AttachmentStorageProperties properties) {
        this.drafts = drafts;
        this.attachments = attachments;
        this.storage = storage;
        this.properties = properties;
    }

    @Transactional
    public SpecBreakdownAttachment upload(
            AuthenticatedUser requester,
            UUID draftId,
            String fileName,
            String contentType,
            long sizeBytes,
            InputStream content) {
        SpecBreakdownDraft draft = requireDraft(requester, draftId);
        validate(fileName, sizeBytes);
        UUID id = UUID.randomUUID();
        String key = "spec-breakdown/drafts/%s/attachments/%s/%s".formatted(draft.id(), id, sanitize(fileName));
        String normalizedContentType =
                contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
        try {
            storage.put(key, content, sizeBytes, normalizedContentType);
        } catch (IOException e) {
            throw new IllegalStateException("Could not store spec breakdown attachment object", e);
        }
        Instant now = Instant.now();
        return attachments.save(
                new SpecBreakdownAttachment(
                        id, draft.id(), fileName, normalizedContentType, key, sizeBytes, requester.userId(), now, null));
    }

    public List<SpecBreakdownAttachment> list(AuthenticatedUser requester, UUID draftId) {
        requireDraft(requester, draftId);
        return attachments.findActiveByDraftId(draftId);
    }

    @Transactional
    public void delete(AuthenticatedUser requester, UUID draftId, UUID attachmentId) {
        requireDraft(requester, draftId);
        SpecBreakdownAttachment attachment = requireAttachment(draftId, attachmentId);
        try {
            storage.delete(attachment.storageKey());
        } catch (IOException ignored) {
            // Metadata tombstone is authoritative; storage cleanup can be retried operationally.
        }
        attachments.save(
                new SpecBreakdownAttachment(
                        attachment.id(),
                        attachment.draftId(),
                        attachment.fileName(),
                        attachment.contentType(),
                        attachment.storageKey(),
                        attachment.sizeBytes(),
                        attachment.createdByUserId(),
                        attachment.createdAt(),
                        Instant.now()));
    }

    private SpecBreakdownDraft requireDraft(AuthenticatedUser requester, UUID draftId) {
        return drafts.getDraft(requester, draftId)
                .orElseThrow(() -> new IllegalArgumentException("Spec breakdown draft not found or access denied"));
    }

    private SpecBreakdownAttachment requireAttachment(UUID draftId, UUID attachmentId) {
        SpecBreakdownAttachment attachment =
                attachments
                        .findActiveById(attachmentId)
                        .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
        if (!attachment.draftId().equals(draftId)) {
            throw new IllegalArgumentException("Attachment not found");
        }
        return attachment;
    }

    private void validate(String fileName, long sizeBytes) {
        if (fileName == null || fileName.isBlank()) throw new IllegalArgumentException("File name is required");
        if (sizeBytes <= 0 || sizeBytes > properties.getMaxSizeBytes())
            throw new IllegalArgumentException("Attachment size exceeds configured limit");
    }

    private String sanitize(String fileName) {
        return fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
