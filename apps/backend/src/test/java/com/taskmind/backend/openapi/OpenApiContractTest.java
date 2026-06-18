package com.taskmind.backend.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class OpenApiContractTest {
    private static final Set<String> DOCUMENTED_ROUTE_FAMILIES =
            Set.of(
                    "/v1/tasks",
                    "/v1/task-links",
                    "/v1/projects",
                    "/v1/projects/{projectId}/members",
                    "/v1/projects/{projectId}/releases",
                    "/v1/projects/{projectId}/ai-brief",
                    "/v1/planner/daily/generate",
                    "/v1/planner/reschedule/proposals",
                    "/v1/review/weekly/generate",
                    "/v1/ai/capture",
                    "/v1/ai/capture/accept",
                    "/v1/ai/capture/reject",
                    "/v1/ai/tasks/describe",
                    "/v1/ai/tasks/describe/autocomplete",
                    "/v1/ai/tasks/translate",
                    "/v1/nova/chat",
                    "/v1/nova/chat/stream",
                    "/v1/nova/capabilities",
                    "/v1/nova/runs/{runId}",
                    "/v1/spec-breakdown",
                    "/v1/scheduler",
                    "/v1/scheduler/ai/duration-estimate",
                    "/v1/scheduler/ai/rationale-phrase");

    private static final Set<String> INTENTIONALLY_UNDOCUMENTED_ROUTES = Set.of("/api/health");

    @Test
    void documentsCurrentlyImplementedRouteFamilies() throws Exception {
        Set<String> pathKeys = openApiPathKeys();

        assertTrue(
                DOCUMENTED_ROUTE_FAMILIES.stream().allMatch(family -> hasPathInFamily(pathKeys, family)),
                missingRoutesMessage(pathKeys));
        assertEquals(
                Set.of("/api/health"),
                INTENTIONALLY_UNDOCUMENTED_ROUTES,
                "Keep the undocumented route allowlist small and intentional.");
    }

    @Test
    void documentsM06SearchAndAttachmentEndpoints() throws Exception {
        Set<String> pathKeys = openApiPathKeys();

        assertTrue(pathKeys.contains("/v1/activity/search"));
        assertTrue(pathKeys.contains("/v1/tasks/{taskId}/attachments"));

        Set<String> schemas = openApiSchemaKeys();
        assertTrue(schemas.contains("TaskAttachment"));
        assertTrue(schemas.contains("ActivitySearchDocument"));
        assertTrue(pathKeys.contains("/v1/spec-breakdown/drafts/{draftId}/attachments"));
        assertTrue(schemas.contains("SpecBreakdownAttachment"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void taskAttachmentSchemaDoesNotExposeObjectStorageKey() throws Exception {
        Map<String, Object> openapi = openApiYaml();
        Map<String, Object> components = (Map<String, Object>) openapi.get("components");
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        Map<String, Object> taskAttachment = (Map<String, Object>) schemas.get("TaskAttachment");
        Iterable<Object> required = (Iterable<Object>) taskAttachment.get("required");
        Map<String, Object> properties = (Map<String, Object>) taskAttachment.get("properties");

        assertFalse(properties.containsKey("objectKey"));
        for (Object requiredProperty : required) {
            assertFalse("objectKey".equals(requiredProperty));
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> openApiPathKeys() throws Exception {
        Map<String, Object> openapi = openApiYaml();
        Map<String, Object> paths = (Map<String, Object>) openapi.get("paths");
        return new TreeSet<>(paths.keySet());
    }

    @SuppressWarnings("unchecked")
    private Set<String> openApiSchemaKeys() throws Exception {
        Map<String, Object> openapi = openApiYaml();
        Map<String, Object> components = (Map<String, Object>) openapi.get("components");
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        return new TreeSet<>(schemas.keySet());
    }

    private Map<String, Object> openApiYaml() throws Exception {
        try (InputStream stream = Files.newInputStream(Path.of("openapi.yaml"))) {
            return new Yaml().load(stream);
        }
    }

    private String missingRoutesMessage(Set<String> pathKeys) {
        Set<String> missing = new TreeSet<>(DOCUMENTED_ROUTE_FAMILIES);
        missing.removeIf(family -> hasPathInFamily(pathKeys, family));
        return "Missing OpenAPI route families: " + missing;
    }

    private boolean hasPathInFamily(Set<String> pathKeys, String routeFamily) {
        return pathKeys.stream()
                .anyMatch(path -> path.equals(routeFamily) || path.startsWith(routeFamily + "/"));
    }
}
