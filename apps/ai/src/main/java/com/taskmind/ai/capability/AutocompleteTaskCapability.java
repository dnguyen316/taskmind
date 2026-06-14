package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AutocompleteTaskCapability extends AbstractTypedCapability {
    public AutocompleteTaskCapability(ObjectMapper objectMapper) {
        super(
                AiCapabilityId.AUTOCOMPLETE_TASK,
                "Suggest task description completions.",
                objectMapper,
                List.of("text"),
                schema(objectMapper, "suggestions"));
    }
}
