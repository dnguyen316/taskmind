package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SpecBreakdownCapability extends AbstractTypedCapability {
    public SpecBreakdownCapability(ObjectMapper objectMapper) {
        super(
                AiCapabilityId.SPEC_BREAKDOWN,
                "Produce a complete Epic, Story, and Subtask hierarchy from a product spec.",
                objectMapper,
                List.of(SpecBreakdownPromptSupport.SPEC_FIELD),
                SpecBreakdownPromptSupport.specSchema(objectMapper, "tree", "metadata", "warnings"));
    }
}
