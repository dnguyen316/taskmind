package com.taskmind.ai.internal;

import com.taskmind.ai.agent.AiAgentRuntime;
import com.taskmind.ai.agent.InvalidCapabilityInputException;
import com.taskmind.ai.activity.ActivitySearchAssistService;
import com.taskmind.ai.agent.UnknownCapabilityException;
import com.taskmind.ai.audit.AiRunAuditRepository;
import com.taskmind.ai.capability.CapabilityDescriptor;
import com.taskmind.ai.capability.CapabilityRegistry;
import com.taskmind.ai.chat.ChatService;
import com.taskmind.ai.chat.InvalidChatRequestException;
import com.taskmind.ai.contracts.AiCapabilityId;
import com.taskmind.ai.contracts.activity.ActivitySearchAssistRequest;
import com.taskmind.ai.contracts.activity.ActivitySearchAssistResponse;
import com.taskmind.ai.contracts.audit.AiRunSummary;
import com.taskmind.ai.contracts.capability.CapabilityError;
import com.taskmind.ai.contracts.capability.CapabilityRequest;
import com.taskmind.ai.contracts.capability.CapabilityResponse;
import com.taskmind.ai.contracts.chat.ChatRequest;
import com.taskmind.ai.contracts.chat.ChatResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class InternalAiController {
    private final ChatService chatService;
    private final AiAgentRuntime aiAgentRuntime;
    private final AiRunAuditRepository auditRepository;
    private final CapabilityRegistry capabilityRegistry;
    private final ActivitySearchAssistService activitySearchAssistService;

    public InternalAiController(
            ChatService chatService,
            AiAgentRuntime aiAgentRuntime,
            AiRunAuditRepository auditRepository,
            CapabilityRegistry capabilityRegistry,
            ActivitySearchAssistService activitySearchAssistService) {
        this.chatService = chatService;
        this.aiAgentRuntime = aiAgentRuntime;
        this.auditRepository = auditRepository;
        this.capabilityRegistry = capabilityRegistry;
        this.activitySearchAssistService = activitySearchAssistService;
    }

    @PostMapping("/internal/ai/chat")
    ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    @PostMapping("/internal/ai/capabilities/{capabilityId}:run")
    CapabilityResponse runCapability(
            @PathVariable String capabilityId, @Valid @RequestBody CapabilityRequest request) {
        CapabilityRequest normalizedRequest =
                new CapabilityRequest(
                        new AiCapabilityId(capabilityId),
                        request.userId(),
                        request.workspaceId(),
                        request.input(),
                        request.correlationId(),
                        request.idempotencyKey());
        return aiAgentRuntime.execute(normalizedRequest);
    }

    @PostMapping("/internal/ai/activity/search/assist")
    ActivitySearchAssistResponse assistActivitySearch(
            @Valid @RequestBody ActivitySearchAssistRequest request) {
        return activitySearchAssistService.assist(request);
    }

    @GetMapping("/internal/ai/runs/{runId}")
    AiRunSummary getRun(@PathVariable UUID runId) {
        return auditRepository
                .findSummary(runId)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "AI run not found"));
    }

    @GetMapping("/internal/ai/capabilities")
    Map<String, List<CapabilityDescriptor>> listCapabilities() {
        return Map.of("capabilities", capabilityRegistry.list());
    }

    @ExceptionHandler(UnknownCapabilityException.class)
    ResponseEntity<CapabilityError> unknownCapability(UnknownCapabilityException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new CapabilityError("UNKNOWN_CAPABILITY", ex.getMessage(), null));
    }

    @ExceptionHandler({
        InvalidCapabilityInputException.class,
        InvalidChatRequestException.class,
        IllegalArgumentException.class
    })
    ResponseEntity<CapabilityError> invalidRequest(RuntimeException ex) {
        return ResponseEntity.badRequest()
                .body(new CapabilityError("INVALID_REQUEST", ex.getMessage(), null));
    }
}
