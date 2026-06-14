package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DashboardInsightsCapability extends AbstractTypedCapability {
    public DashboardInsightsCapability(ObjectMapper objectMapper) {
        super(
                AiCapabilityId.DASHBOARD_INSIGHTS,
                "Generate dashboard insights from user context.",
                objectMapper,
                List.of("userId"),
                schema(objectMapper, "userId", "summary", "insights", "recommendations"));
    }
}
