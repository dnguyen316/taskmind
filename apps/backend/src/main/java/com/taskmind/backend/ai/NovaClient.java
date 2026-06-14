package com.taskmind.backend.ai;

import com.taskmind.ai.contracts.audit.AiRunSummary;
import com.taskmind.ai.contracts.capability.CapabilitiesResponse;
import com.taskmind.ai.contracts.capability.CapabilityRequest;
import com.taskmind.ai.contracts.capability.CapabilityResponse;
import com.taskmind.ai.contracts.chat.ChatRequest;
import com.taskmind.ai.contracts.chat.ChatResponse;
import java.util.UUID;

public interface NovaClient {
    ChatResponse chat(ChatRequest request);

    CapabilityResponse executeCapability(String capabilityId, CapabilityRequest request);

    CapabilitiesResponse capabilities();

    AiRunSummary run(UUID runId);
}
