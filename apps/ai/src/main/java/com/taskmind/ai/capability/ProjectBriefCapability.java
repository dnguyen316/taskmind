package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProjectBriefCapability extends AbstractTypedCapability {
    public ProjectBriefCapability(ObjectMapper objectMapper) {
        super(
                AiCapabilityId.PROJECT_BRIEF,
                "Generate a concise project brief from project state.",
                objectMapper,
                List.of("projectId"),
                schema(
                        objectMapper,
                        "projectId",
                        "summary",
                        "currentFocus",
                        "risks",
                        "suggestedNextSteps"));
    }
}
