package com.taskmind.ai.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.audit.AiRunAuditRepository;
import com.taskmind.ai.audit.AiRunRecord;
import com.taskmind.ai.contracts.AiCapabilityId;
import com.taskmind.ai.contracts.chat.ChatMessage;
import com.taskmind.ai.contracts.chat.ChatRequest;
import com.taskmind.ai.contracts.chat.ChatResponse;
import com.taskmind.ai.provider.AiProvider;
import com.taskmind.ai.provider.ProviderRequest;
import com.taskmind.ai.provider.ProviderResponse;
import com.taskmind.ai.provider.ProviderRouter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ChatService {
    private final ChatSessionStore chatSessionStore;
    private final ProviderRouter providerRouter;
    private final AiRunAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public ChatService(
            ChatSessionStore chatSessionStore,
            ProviderRouter providerRouter,
            AiRunAuditRepository auditRepository,
            ObjectMapper objectMapper) {
        this.chatSessionStore = chatSessionStore;
        this.providerRouter = providerRouter;
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    public ChatResponse chat(ChatRequest request) {
        if (!StringUtils.hasText(request.message())) {
            throw new InvalidChatRequestException("Chat message is required");
        }
        String sessionId =
                StringUtils.hasText(request.sessionId())
                        ? request.sessionId()
                        : UUID.randomUUID().toString();
        ChatSession existing =
                chatSessionStore.find(sessionId).orElse(new ChatSession(sessionId, List.of()));
        List<ChatMessage> messages = new ArrayList<>(existing.messages());
        messages.add(new ChatMessage("user", request.message(), Instant.now()));
        AiProvider provider = providerRouter.defaultProvider();
        UUID runId =
                auditRepository.start(
                        new AiRunRecord(
                                null,
                                null,
                                AiCapabilityId.CHAT,
                                provider.id(),
                                provider.modelId(),
                                requestHash(sessionId, request.message()),
                                objectMapper.createObjectNode().put("message", request.message()),
                                request.correlationId()));
        ProviderResponse providerResponse =
                provider.complete(
                        new ProviderRequest(
                                AiCapabilityId.CHAT,
                                objectMapper.createObjectNode().put("message", request.message()),
                                contents(messages),
                                request.correlationId()));
        messages.add(new ChatMessage("assistant", providerResponse.message(), Instant.now()));
        chatSessionStore.save(new ChatSession(sessionId, messages));
        auditRepository.succeed(
                runId,
                providerResponse.output(),
                providerResponse.promptTokens(),
                providerResponse.completionTokens(),
                providerResponse.totalTokens(),
                providerResponse.latencyMs());
        return new ChatResponse(sessionId, providerResponse.message(), runId, List.of());
    }

    private List<String> contents(List<ChatMessage> messages) {
        return messages.stream().map(message -> message.role() + ":" + message.content()).toList();
    }

    private String requestHash(String sessionId, String message) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash =
                    digest.digest((sessionId + ":" + message).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
