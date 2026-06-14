package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DurationEstimateCapability extends AbstractTypedCapability {
    public DurationEstimateCapability(ObjectMapper objectMapper) {
        super(
                AiCapabilityId.DURATION_ESTIMATE,
                "Estimate task duration with rationale.",
                objectMapper,
                List.of("title"),
                schema(objectMapper, "durationMinutes", "rationale", "confidence"));
    }
}
