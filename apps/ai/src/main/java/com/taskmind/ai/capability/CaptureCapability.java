package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CaptureCapability extends AbstractTypedCapability {
    public CaptureCapability(ObjectMapper objectMapper) {
        super(
                AiCapabilityId.CAPTURE,
                "Draft a structured capture proposal from user input.",
                objectMapper,
                List.of("text"),
                schema(objectMapper, "drafts", "clarificationQuestion"));
    }
}
