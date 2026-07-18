package com.taskmind.backend.attachment.interfaces.rest;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.attachment.domain.repository.ObjectStoragePort;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.StreamUtils;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(com.taskmind.backend.security.TestJwtSupport.Config.class)
class TaskAttachmentControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired FailureInjectingObjectStoragePort storage;

    @BeforeEach
    void resetStorageFailures() {
        storage.resetFailures();
    }

    @Test
    void uploadsListsDownloadsAndDeletesAttachmentForTaskOwner() throws Exception {
        String userId = UUID.randomUUID().toString();
        String taskId = createTask(userId);
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "note.txt", "text/plain", "hello attachment".getBytes());

        var upload =
                mockMvc.perform(
                                multipart("/v1/tasks/{taskId}/attachments", taskId)
                                        .file(file)
                                        .param("mediaKind", "DOCUMENT")
                                        .with(jwt(userId)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.fileName").value("note.txt"))
                        .andExpect(jsonPath("$.mediaKind").value("DOCUMENT"))
                        .andExpect(jsonPath("$.objectKey").doesNotExist())
                        .andReturn();
        String attachmentId =
                objectMapper.readTree(upload.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/v1/tasks/{taskId}/attachments", taskId).with(jwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(attachmentId))
                .andExpect(jsonPath("$[0].objectKey").doesNotExist());

        mockMvc.perform(
                        get(
                                        "/v1/tasks/{taskId}/attachments/{attachmentId}/download",
                                        taskId,
                                        attachmentId)
                                .with(jwt(userId)))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        org.hamcrest.Matchers.containsString("note.txt")))
                .andExpect(content().bytes("hello attachment".getBytes()));

        mockMvc.perform(
                        delete(
                                        "/v1/tasks/{taskId}/attachments/{attachmentId}",
                                        taskId,
                                        attachmentId)
                                .with(jwt(userId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/tasks/{taskId}/attachments", taskId).with(jwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void downloadContentLengthMatchesAttachmentMetadata() throws Exception {
        String userId = UUID.randomUUID().toString();
        String taskId = createTask(userId);
        byte[] content = "streamed content length".getBytes();
        MockMultipartFile file =
                new MockMultipartFile("file", "length.txt", "text/plain", content);

        var upload =
                mockMvc.perform(
                                multipart("/v1/tasks/{taskId}/attachments", taskId)
                                        .file(file)
                                        .param("mediaKind", "DOCUMENT")
                                        .with(jwt(userId)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.sizeBytes").value(content.length))
                        .andReturn();
        String attachmentId =
                objectMapper.readTree(upload.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(
                        get(
                                        "/v1/tasks/{taskId}/attachments/{attachmentId}/download",
                                        taskId,
                                        attachmentId)
                                .with(jwt(userId)))
                .andExpect(status().isOk())
                .andExpect(header().longValue("Content-Length", content.length));
    }

    @Test
    void downloadStorageFailureReturnsServiceUnavailable() throws Exception {
        String userId = UUID.randomUUID().toString();
        String taskId = createTask(userId);
        MockMultipartFile file =
                new MockMultipartFile("file", "note.txt", "text/plain", "content".getBytes());
        var upload =
                mockMvc.perform(
                                multipart("/v1/tasks/{taskId}/attachments", taskId)
                                        .file(file)
                                        .param("mediaKind", "DOCUMENT")
                                        .with(jwt(userId)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String attachmentId =
                objectMapper.readTree(upload.getResponse().getContentAsString()).get("id").asText();
        storage.failGet = true;

        mockMvc.perform(
                        get(
                                        "/v1/tasks/{taskId}/attachments/{attachmentId}/download",
                                        taskId,
                                        attachmentId)
                                .with(jwt(userId)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void storageFailureReturnsServiceUnavailableAndDoesNotPersistMetadata() throws Exception {
        String userId = UUID.randomUUID().toString();
        String taskId = createTask(userId);
        MockMultipartFile file =
                new MockMultipartFile("file", "note.txt", "text/plain", "content".getBytes());
        storage.failPut = true;

        mockMvc.perform(
                        multipart("/v1/tasks/{taskId}/attachments", taskId)
                                .file(file)
                                .param("mediaKind", "DOCUMENT")
                                .with(jwt(userId)))
                .andExpect(status().isServiceUnavailable());

        mockMvc.perform(get("/v1/tasks/{taskId}/attachments", taskId).with(jwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsUploadWithMissingFilename() throws Exception {
        String userId = UUID.randomUUID().toString();
        String taskId = createTask(userId);
        MockMultipartFile file = new MockMultipartFile("file", "", "text/plain", "content".getBytes());

        mockMvc.perform(
                        multipart("/v1/tasks/{taskId}/attachments", taskId)
                                .file(file)
                                .param("mediaKind", "DOCUMENT")
                                .with(jwt(userId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUploadWithMissingMediaKind() throws Exception {
        String userId = UUID.randomUUID().toString();
        String taskId = createTask(userId);
        MockMultipartFile file =
                new MockMultipartFile("file", "note.txt", "text/plain", "content".getBytes());

        mockMvc.perform(
                        multipart("/v1/tasks/{taskId}/attachments", taskId)
                                .file(file)
                                .with(jwt(userId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUploadWithInvalidMediaKind() throws Exception {
        String userId = UUID.randomUUID().toString();
        String taskId = createTask(userId);
        MockMultipartFile file =
                new MockMultipartFile("file", "note.txt", "text/plain", "content".getBytes());

        mockMvc.perform(
                        multipart("/v1/tasks/{taskId}/attachments", taskId)
                                .file(file)
                                .param("mediaKind", "NOT_A_KIND")
                                .with(jwt(userId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsOversizeUploadAsPayloadTooLarge() throws Exception {
        String userId = UUID.randomUUID().toString();
        String taskId = createTask(userId);
        byte[] content = new byte[1_048_577];
        MockMultipartFile file =
                new MockMultipartFile("file", "large.bin", "application/octet-stream", content);

        mockMvc.perform(
                        multipart("/v1/tasks/{taskId}/attachments", taskId)
                                .file(file)
                                .param("mediaKind", "DOCUMENT")
                                .with(jwt(userId)))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void hidesAttachmentTaskExistenceFromNonOwner() throws Exception {
        String ownerId = UUID.randomUUID().toString();
        String taskId = createTask(ownerId);
        mockMvc.perform(
                        get("/v1/tasks/{taskId}/attachments", taskId)
                                .with(jwt(UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
    }

    private String createTask(String userId) throws Exception {
        String payload =
                """
            {"userId":"%s","title":"Attachment task","status":"TODO","priority":2,"source":"MANUAL"}
            """
                        .formatted(userId);
        var response =
                mockMvc.perform(
                                post("/v1/tasks")
                                        .with(jwt(userId))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(payload))
                        .andExpect(status().isCreated())
                        .andReturn();
        return objectMapper
                .readTree(response.getResponse().getContentAsString())
                .get("id")
                .asText();
    }

    @TestConfiguration
    static class StorageFailureTestConfig {
        @Bean
        @Primary
        FailureInjectingObjectStoragePort failureInjectingObjectStoragePort() {
            return new FailureInjectingObjectStoragePort();
        }
    }

    static class FailureInjectingObjectStoragePort implements ObjectStoragePort {
        private final Map<String, StoredObject> objects = new ConcurrentHashMap<>();
        volatile boolean failPut;
        volatile boolean failGet;

        @Override
        public void put(String key, InputStream content, long sizeBytes, String contentType)
                throws IOException {
            if (failPut) {
                throw new RuntimeException("storage unavailable");
            }
            byte[] bytes = StreamUtils.copyToByteArray(content);
            objects.put(key, new StoredObject(new ByteArrayResource(bytes), contentType, sizeBytes));
        }

        @Override
        public StoredObject get(String key) throws IOException {
            if (failGet) {
                throw new RuntimeException("storage unavailable");
            }
            StoredObject object = objects.get(key);
            if (object == null) {
                throw new IOException("Object not found");
            }
            return object;
        }

        @Override
        public void delete(String key) {
            objects.remove(key);
        }

        void resetFailures() {
            failPut = false;
            failGet = false;
        }
    }
}
