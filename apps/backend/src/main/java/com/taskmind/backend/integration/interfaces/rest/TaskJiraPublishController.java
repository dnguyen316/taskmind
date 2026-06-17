package com.taskmind.backend.integration.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.integration.application.IntegrationPublishApplicationService;
import com.taskmind.backend.integration.domain.model.IntegrationPublishRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/tasks/{taskId}/integrations")
public class TaskJiraPublishController {
    private final IntegrationPublishApplicationService publish;
    public TaskJiraPublishController(IntegrationPublishApplicationService publish) { this.publish = publish; }
    @PostMapping("/jira/publish") public PublishResponse publishToJira(@PathVariable UUID taskId, AuthenticatedUser actor, @Valid @RequestBody PublishRequest request) { try { return PublishResponse.from(publish.publish(actor, taskId, request.projectLinkId())); } catch (IllegalArgumentException e) { throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage(), e); } }
    @PostMapping("/wiki/publish") public PublishResponse publishToWiki(@PathVariable UUID taskId, AuthenticatedUser actor, @Valid @RequestBody PublishRequest request) { try { return PublishResponse.from(publish.publish(actor, taskId, request.projectLinkId())); } catch (IllegalArgumentException e) { throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage(), e); } }
    public record PublishRequest(@NotNull UUID projectLinkId) {}
    public record PublishResponse(UUID id, UUID taskId, UUID projectLinkId, String provider, String externalId, String externalKey, String externalUrl, String status) { static PublishResponse from(IntegrationPublishRecord r) { return new PublishResponse(r.id(), r.taskId(), r.projectLinkId(), r.provider().name(), r.externalId(), r.externalKey(), r.externalUrl(), r.status()); } }
}
