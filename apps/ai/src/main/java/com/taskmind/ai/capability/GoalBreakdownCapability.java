package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GoalBreakdownCapability extends AbstractTypedCapability {
    public GoalBreakdownCapability(ObjectMapper objectMapper) {
        super(
                AiCapabilityId.GOAL_BREAKDOWN,
                "Break a goal into milestones and draft tasks.",
                objectMapper,
                List.of("goalId"),
                schema(objectMapper, "goalId", "milestones", "tasks", "riskNotes"));
    }
}
