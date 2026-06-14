package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TranslateTaskCapability extends AbstractTypedCapability {
    public TranslateTaskCapability(ObjectMapper objectMapper) {
        super(
                AiCapabilityId.TRANSLATE_TASK,
                "Translate task text into a requested language.",
                objectMapper,
                List.of("text", "targetLanguage"),
                schema(objectMapper, "translatedText", "targetLanguage"));
    }
}
