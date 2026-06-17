package com.taskmind.backend.relay;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RelayContextClient implements RelayContextPort {
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST =
            new ParameterizedTypeReference<>() {};
    private final RestClient restClient;

    public RelayContextClient(RestClient relayRestClient) {
        this.restClient = relayRestClient;
    }

    public List<Map<String, Object>> userTasks(UUID userId) {
        return get("/internal/context/users/{userId}/tasks", userId);
    }

    public List<Map<String, Object>> projectMetrics(UUID projectId) {
        return get("/internal/context/projects/{projectId}/metrics", projectId);
    }

    private List<Map<String, Object>> get(String uri, Object id) {
        try {
            List<Map<String, Object>> body = restClient.get().uri(uri, id).retrieve().body(LIST);
            return body == null ? List.of() : body;
        } catch (RestClientException e) {
            throw new RelayClientException("Relay context API is unavailable", e);
        }
    }
}
