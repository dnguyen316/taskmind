package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class SpecBreakdownPromptSupport {
    static final String SPEC_FIELD = "specText";
    static final String DRAFT_FIELD = "draftTree";
    static final String SECTION_FIELD = "sectionText";

    private SpecBreakdownPromptSupport() {}

    static JsonNode specSchema(ObjectMapper objectMapper, String... requiredFields) {
        return AbstractTypedCapability.schema(objectMapper, requiredFields);
    }
}
