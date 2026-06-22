package com.taskmind.relay.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ActivityEventIndexMappingTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void documentsAutocompleteAndNormalizedRecommendationFields() throws Exception {
        JsonNode mapping = objectMapper.readTree(ActivityEventIndexMapping.MAPPING_JSON);
        JsonNode properties = mapping.path("mappings").path("properties");

        assertThat(properties.path("title").path("fields").path("autocomplete").path("type").asText())
                .isEqualTo("search_as_you_type");
        assertThat(properties.path("entityTypeKeyword").path("type").asText()).isEqualTo("keyword");
        assertThat(properties.path("eventTypeKeyword").path("normalizer").asText()).isEqualTo("lowercase_keyword");
        assertThat(properties.path("statusKeyword").path("normalizer").asText()).isEqualTo("lowercase_keyword");
        assertThat(properties.path("payloadText").path("type").asText()).isEqualTo("text");
    }
}
