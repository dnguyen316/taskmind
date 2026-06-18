package com.taskmind.backend.specbreakdown.attachment;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.specbreakdown.application.SpecBreakdownAttachmentApplicationService;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownAttachment;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/spec-breakdown/drafts/{draftId}/attachments")
public class SpecBreakdownAttachmentController {
    private final SpecBreakdownAttachmentApplicationService service;

    public SpecBreakdownAttachmentController(SpecBreakdownAttachmentApplicationService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Attachment> upload(
            AuthenticatedUser requester,
            @PathVariable UUID draftId,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(
                            toResponse(
                                    service.upload(
                                            requester,
                                            draftId,
                                            file.getOriginalFilename(),
                                            file.getContentType(),
                                            file.getSize(),
                                            file.getInputStream())));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read upload", e);
        }
    }

    @GetMapping
    public List<Attachment> list(AuthenticatedUser requester, @PathVariable UUID draftId) {
        try {
            return service.list(requester, draftId).stream().map(this::toResponse).toList();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        }
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(
            AuthenticatedUser requester,
            @PathVariable UUID draftId,
            @PathVariable UUID attachmentId) {
        try {
            service.delete(requester, draftId, attachmentId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    private Attachment toResponse(SpecBreakdownAttachment attachment) {
        return new Attachment(
                attachment.id(),
                attachment.draftId(),
                attachment.fileName(),
                attachment.contentType(),
                attachment.sizeBytes(),
                attachment.createdByUserId(),
                attachment.createdAt(),
                attachment.deletedAt());
    }

    public record Attachment(
            UUID id,
            UUID draftId,
            String fileName,
            String contentType,
            long sizeBytes,
            UUID createdByUserId,
            Instant createdAt,
            Instant deletedAt) {}
}
