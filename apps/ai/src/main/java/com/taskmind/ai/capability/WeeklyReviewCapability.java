package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WeeklyReviewCapability extends AbstractTypedCapability {
    public WeeklyReviewCapability(ObjectMapper objectMapper) {
        super(
                AiCapabilityId.WEEKLY_REVIEW,
                "Draft weekly review insights from context.",
                objectMapper,
                List.of("userId"),
                schema(
                        objectMapper,
                        "userId",
                        "summary",
                        "slippageInsights",
                        "recommendations",
                        "nextWeekPriorities"));
    }
}
