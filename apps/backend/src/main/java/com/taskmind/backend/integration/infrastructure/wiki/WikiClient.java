package com.taskmind.backend.integration.infrastructure.wiki;

import static com.taskmind.backend.integration.infrastructure.ProviderHttpSupport.map;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class WikiClient {
    private final RestClient restClient;

    public WikiClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public PublishedPage publish(String baseUrl, String accessToken, String spaceKey, String title) {
        try {
            JsonNode response = restClient.post()
                    .uri(baseUrl, uri -> uri.path("/wiki/rest/api/content").build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CreatePageRequest("page", title, new Space(spaceKey), new Body(new Storage("", "storage"))))
                    .retrieve()
                    .body(JsonNode.class);
            String id = response.path("id").asText();
            String key = spaceKey + ":" + title;
            String webui = response.path("_links").path("webui").asText("/pages/" + id);
            return new PublishedPage(id, key, baseUrl.replaceAll("/$", "") + webui);
        } catch (RestClientException exception) {
            throw map("Wiki", exception);
        }
    }

    private record CreatePageRequest(String type, String title, Space space, Body body) {}
    private record Space(String key) {}
    private record Body(Storage storage) {}
    private record Storage(String value, String representation) {}
    public record PublishedPage(String id, String key, String url) {}
}
