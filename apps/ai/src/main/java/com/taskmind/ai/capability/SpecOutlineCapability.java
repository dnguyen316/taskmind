package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SpecOutlineCapability extends AbstractTypedCapability {
    public SpecOutlineCapability(ObjectMapper objectMapper) {
        super(
                AiCapabilityId.SPEC_OUTLINE,
                "Outline a product spec into draft epics and stories.",
                objectMapper,
                List.of(SpecBreakdownPromptSupport.SPEC_FIELD),
                SpecBreakdownPromptSupport.specSchema(objectMapper, "epics", "stories", "warnings"));
    }
}
