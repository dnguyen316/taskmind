package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RationalePhraseCapability extends AbstractTypedCapability {
    public RationalePhraseCapability(ObjectMapper objectMapper) {
        super(
                AiCapabilityId.RATIONALE_PHRASE,
                "Generate a short schedule rationale phrase.",
                objectMapper,
                List.of("title"),
                schema(objectMapper, "rationale"));
    }
}
