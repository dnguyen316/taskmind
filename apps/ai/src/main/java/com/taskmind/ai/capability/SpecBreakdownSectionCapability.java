package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SpecBreakdownSectionCapability extends AbstractTypedCapability {
    public SpecBreakdownSectionCapability(ObjectMapper objectMapper) {
        super(
                AiCapabilityId.SPEC_BREAKDOWN_SECTION,
                "Break down one selected spec section into draft backlog items.",
                objectMapper,
                List.of(SpecBreakdownPromptSupport.SECTION_FIELD),
                SpecBreakdownPromptSupport.specSchema(objectMapper, "sectionTitle", "items", "warnings"));
    }
}
