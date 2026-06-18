package com.taskmind.backend.attachment.interfaces.rest;

import com.taskmind.backend.attachment.application.TaskAttachmentApplicationService;
import com.taskmind.backend.attachment.domain.model.MediaKind;
import com.taskmind.backend.attachment.domain.model.TaskAttachment;
import com.taskmind.backend.attachment.interfaces.rest.dto.TaskAttachmentResponse;
import com.taskmind.backend.auth.AuthenticatedUser;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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

@RestController
@RequestMapping("/v1/tasks/{taskId}/attachments")
public class TaskAttachmentController {
    private final TaskAttachmentApplicationService service;

    public TaskAttachmentController(TaskAttachmentApplicationService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskAttachmentResponse> upload(
            AuthenticatedUser requester,
            @PathVariable UUID taskId,
            @RequestParam("file") MultipartFile file,
            @RequestParam MediaKind mediaKind) {
        try {
            TaskAttachment attachment =
                    service.upload(
                            requester,
                            taskId,
                            file.getOriginalFilename(),
                            file.getContentType(),
                            file.getSize(),
                            mediaKind,
                            file.getInputStream());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(attachment));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read upload", e);
        }
    }

    @GetMapping
    public List<TaskAttachmentResponse> list(AuthenticatedUser requester, @PathVariable UUID taskId) {
        try {
            return service.list(requester, taskId).stream().map(this::toResponse).toList();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        }
    }


    private TaskAttachmentResponse toResponse(TaskAttachment attachment) {
        TaskAttachmentResponse response = new TaskAttachmentResponse();
        response.id = attachment.id();
        response.version = attachment.version();
        response.taskId = attachment.taskId();
        response.ownerUserId = attachment.ownerUserId();
        response.fileName = attachment.fileName();
        response.contentType = attachment.contentType();
        response.sizeBytes = attachment.sizeBytes();
        response.mediaKind = attachment.mediaKind();
        response.deletedAt = attachment.deletedAt();
        response.createdAt = attachment.createdAt();
        response.updatedAt = attachment.updatedAt();
        return response;
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(
            AuthenticatedUser requester,
            @PathVariable UUID taskId,
            @PathVariable UUID attachmentId) {
        try {
            TaskAttachmentApplicationService.Download download =
                    service.download(requester, taskId, attachmentId);
            return ResponseEntity.ok()
                    .contentType(
                            MediaType.parseMediaType(
                                    download.contentType() == null
                                            ? "application/octet-stream"
                                            : download.contentType()))
                    .contentLength(download.sizeBytes())
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename(download.attachment().fileName())
                                    .build()
                                    .toString())
                    .body(download.resource());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(
            AuthenticatedUser requester,
            @PathVariable UUID taskId,
            @PathVariable UUID attachmentId) {
        try {
            service.delete(requester, taskId, attachmentId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
}
