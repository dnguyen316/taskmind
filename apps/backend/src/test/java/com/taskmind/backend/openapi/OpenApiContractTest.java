package com.taskmind.backend.openapi;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OpenApiContractTest {
    @Test
    void documentsM06SearchAndAttachmentEndpoints() throws Exception {
        String openapi = Files.readString(Path.of("openapi.yaml"));
        assertTrue(openapi.contains("/v1/activity/search"));
        assertTrue(openapi.contains("/v1/tasks/{taskId}/attachments"));
        assertTrue(openapi.contains("TaskAttachment"));
        assertTrue(openapi.contains("ActivitySearchDocument"));
    }
}
