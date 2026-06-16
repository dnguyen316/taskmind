package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SpecEnrichCapability extends AbstractTypedCapability {
    public SpecEnrichCapability(ObjectMapper objectMapper) {
        super(
                AiCapabilityId.SPEC_ENRICH,
                "Enrich a spec breakdown draft with acceptance notes, risks, estimates, and labels.",
                objectMapper,
                List.of(SpecBreakdownPromptSupport.SPEC_FIELD),
                SpecBreakdownPromptSupport.specSchema(objectMapper, "items", "risks", "labels"));
    }
}
