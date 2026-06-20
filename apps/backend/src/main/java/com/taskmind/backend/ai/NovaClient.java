package com.taskmind.backend.ai;

import com.taskmind.ai.contracts.activity.ActivitySearchAssistRequest;
import com.taskmind.ai.contracts.activity.ActivitySearchAssistResponse;
import com.taskmind.ai.contracts.audit.AiRunSummary;
import com.taskmind.ai.contracts.capability.CapabilitiesResponse;
import com.taskmind.ai.contracts.capability.CapabilityRequest;
import com.taskmind.ai.contracts.capability.CapabilityResponse;
import com.taskmind.ai.contracts.chat.ChatRequest;
import com.taskmind.ai.contracts.chat.ChatResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public interface NovaClient {
    ChatResponse chat(ChatRequest request);

    void chatStream(ChatRequest request, OutputStream outputStream) throws IOException;

    CapabilityResponse executeCapability(String capabilityId, CapabilityRequest request);

    default ActivitySearchAssistResponse assistActivitySearch(ActivitySearchAssistRequest request) {
        throw new UnsupportedOperationException("Activity search assist is not implemented");
    }

    CapabilitiesResponse capabilities();

    AiRunSummary run(UUID runId);
}
