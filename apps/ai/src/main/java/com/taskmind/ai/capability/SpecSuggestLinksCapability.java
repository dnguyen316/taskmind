package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SpecSuggestLinksCapability extends AbstractTypedCapability {
    public SpecSuggestLinksCapability(ObjectMapper objectMapper) {
        super(
                AiCapabilityId.SPEC_SUGGEST_LINKS,
                "Suggest related tasks, projects, docs, and dependencies for a spec draft.",
                objectMapper,
                List.of(SpecBreakdownPromptSupport.SPEC_FIELD),
                SpecBreakdownPromptSupport.specSchema(objectMapper, "links", "dependencies", "warnings"));
    }
}
