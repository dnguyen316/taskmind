package com.taskmind.relay.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(prefix = "taskmind.activity-search", name = "enabled", havingValue = "true")
public class ElasticsearchConfig {
    @Bean
    ActivityEventSearchRepository activityEventSearchRepository(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${spring.elasticsearch.uris:http://localhost:9200}") String elasticsearchUri,
            @Value("${taskmind.activity-search.index:activity-events}") String indexName) {
        RestClient client = builder.baseUrl(elasticsearchUri).build();
        return document -> client.put()
                .uri("/{index}/_doc/{id}", indexName, document.eventId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(document)
                .retrieve()
                .toBodilessEntity();
    }
}
