package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DescribeTaskCapability extends AbstractTypedCapability {
    public DescribeTaskCapability(ObjectMapper objectMapper) {
        super(
                AiCapabilityId.DESCRIBE_TASK,
                "Draft a task description from a title and notes.",
                objectMapper,
                List.of("title"),
                schema(objectMapper, "description", "rationale"));
    }
}
