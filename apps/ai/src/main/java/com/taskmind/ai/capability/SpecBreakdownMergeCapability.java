package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SpecBreakdownMergeCapability extends AbstractTypedCapability {
    public SpecBreakdownMergeCapability(ObjectMapper objectMapper) {
        super(
                AiCapabilityId.SPEC_MERGE,
                "Merge AI spec output into a user-edited draft tree without discarding edits.",
                objectMapper,
                List.of(SpecBreakdownPromptSupport.DRAFT_FIELD),
                SpecBreakdownPromptSupport.specSchema(objectMapper, "mergedTree", "conflicts", "warnings"));
    }
}
