package com.taskmind.backend.ai;

import com.taskmind.ai.contracts.audit.AiRunSummary;
import com.taskmind.ai.contracts.capability.CapabilitiesResponse;
import com.taskmind.ai.contracts.chat.ChatRequest;
import com.taskmind.ai.contracts.chat.ChatResponse;
import java.util.UUID;

public interface NovaClient {
    ChatResponse chat(ChatRequest request);

    CapabilitiesResponse capabilities();

    AiRunSummary run(UUID runId);
}
