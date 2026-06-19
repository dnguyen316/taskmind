package com.taskmind.backend.activity.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(prefix = "taskmind.activity-search", name = "enabled", havingValue = "true")
public class ActivitySearchElasticsearchConfig {
    @Bean
    ActivitySearchRepository activitySearchRepository(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${spring.elasticsearch.uris:http://localhost:9200}") String elasticsearchUri,
            @Value("${taskmind.activity-search.index:activity-events}") String indexName) {
        return new ElasticsearchActivitySearchRepository(
                builder.baseUrl(elasticsearchUri).build(), objectMapper, indexName);
    }

    static class ElasticsearchActivitySearchRepository implements ActivitySearchRepository {
        private final RestClient client;
        private final ObjectMapper objectMapper;
        private final String indexName;

        ElasticsearchActivitySearchRepository(
                RestClient client, ObjectMapper objectMapper, String indexName) {
            this.client = client;
            this.objectMapper = objectMapper;
            this.indexName = indexName;
        }

        @Override
        public boolean enabled() {
            return true;
        }

        @Override
        public List<ActivitySearchDocument> search(UUID userId, String query, int size) {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("size", size);
            ObjectNode bool = root.putObject("query").putObject("bool");
            ArrayNode filter = bool.putArray("filter");
            filter.addObject().putObject("term").put("userId", userId.toString());
            if (query == null || query.isBlank()) {
                bool.set("must", objectMapper.createArrayNode().addObject().putObject("match_all"));
            } else {
                bool.putArray("must")
                        .addObject()
                        .putObject("multi_match")
                        .put("query", query)
                        .putArray("fields")
                        .add("title^2")
                        .add("eventType")
                        .add("status")
                        .add("payloadText");
            }
            root.putObject("sort").putObject("occurredAt").put("order", "desc");
            JsonNode response =
                    client.post()
                            .uri("/{index}/_search", indexName)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(root)
                            .retrieve()
                            .body(JsonNode.class);
            List<ActivitySearchDocument> documents = new ArrayList<>();
            if (response == null) return documents;
            for (JsonNode hit : response.path("hits").path("hits")) {
                JsonNode source = hit.path("_source");
                documents.add(
                        new ActivitySearchDocument(
                                uuid(source, "eventId"),
                                source.path("eventType").asText(),
                                uuid(source, "actorUserId"),
                                uuid(source, "userId"),
                                uuid(source, "projectId"),
                                source.path("entityType").asText(),
                                uuid(source, "entityId"),
                                source.path("title").asText(""),
                                source.path("status").asText(""),
                                source.path("payload"),
                                Instant.parse(source.path("occurredAt").asText())));
            }
            return documents;
        }

        @Override
        public List<String> suggest(UUID userId, String query, int size) {
            if (query == null || query.isBlank()) {
                return List.of();
            }

            String trimmedQuery = query.trim();
            ObjectNode root = objectMapper.createObjectNode();
            root.put("size", Math.min(size * 3, 100));
            ObjectNode bool = root.putObject("query").putObject("bool");
            bool.putArray("filter").addObject().putObject("term").put("userId", userId.toString());
            bool.putArray("must")
                    .addObject()
                    .putObject("multi_match")
                    .put("query", trimmedQuery)
                    .put("type", "bool_prefix")
                    .putArray("fields")
                    .add("title^3")
                    .add("eventType")
                    .add("status")
                    .add("payloadText");
            root.putArray("_source").add("title").add("eventType").add("status").add("payloadText");

            JsonNode response =
                    client.post()
                            .uri("/{index}/_search", indexName)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(root)
                            .retrieve()
                            .body(JsonNode.class);

            Set<String> suggestions = new LinkedHashSet<>();
            if (response == null) {
                return List.of();
            }
            for (JsonNode hit : response.path("hits").path("hits")) {
                JsonNode source = hit.path("_source");
                addSuggestion(suggestions, source.path("title").asText(null), trimmedQuery, size);
                addSuggestion(suggestions, source.path("eventType").asText(null), trimmedQuery, size);
                addSuggestion(suggestions, source.path("status").asText(null), trimmedQuery, size);
                if (suggestions.size() >= size) {
                    break;
                }
            }
            return List.copyOf(suggestions);
        }

        private void addSuggestion(
                Set<String> suggestions, String candidate, String query, int maxSuggestions) {
            if (candidate == null || candidate.isBlank() || suggestions.size() >= maxSuggestions) {
                return;
            }
            String normalizedCandidate = candidate.trim();
            if (normalizedCandidate.toLowerCase().contains(query.toLowerCase())) {
                suggestions.add(normalizedCandidate);
            }
        }

        private UUID uuid(JsonNode node, String field) {
            return node.hasNonNull(field) ? UUID.fromString(node.path(field).asText()) : null;
        }
    }
}
