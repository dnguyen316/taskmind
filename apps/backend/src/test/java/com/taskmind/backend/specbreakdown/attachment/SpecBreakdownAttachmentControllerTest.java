package com.taskmind.backend.specbreakdown.attachment;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.attachment.domain.repository.ObjectStoragePort;
import com.taskmind.backend.specbreakdown.domain.repository.SpecBreakdownAttachmentRepository;
import java.io.InputStream;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(com.taskmind.backend.security.TestJwtSupport.Config.class)
class SpecBreakdownAttachmentControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired SpecBreakdownAttachmentRepository attachments;
    @MockBean ObjectStoragePort storage;

    @Test
    void uploadPersistsMetadataWritesObjectListsByDraftAndSoftDeletes() throws Exception {
        String userId = UUID.randomUUID().toString();
        String draftId = createDraft(userId);
        String otherDraftId = createDraft(userId);
        MockMultipartFile file =
                new MockMultipartFile("file", "spec note.txt", "text/plain", "break it down".getBytes());

        var upload =
                mockMvc.perform(
                                multipart("/v1/spec-breakdown/drafts/{draftId}/attachments", draftId)
                                        .file(file)
                                        .with(jwt(userId)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.draftId").value(draftId))
                        .andExpect(jsonPath("$.fileName").value("spec note.txt"))
                        .andExpect(jsonPath("$.contentType").value("text/plain"))
                        .andExpect(jsonPath("$.sizeBytes").value(13))
                        .andExpect(jsonPath("$.createdByUserId").value(userId))
                        .andExpect(jsonPath("$.storageKey").doesNotExist())
                        .andReturn();
        String attachmentId = objectMapper.readTree(upload.getResponse().getContentAsString()).get("id").asText();
        var persisted = attachments.findActiveById(UUID.fromString(attachmentId)).orElseThrow();
        verify(storage).put(eq(persisted.storageKey()), any(InputStream.class), eq(13L), eq("text/plain"));

        mockMvc.perform(get("/v1/spec-breakdown/drafts/{draftId}/attachments", draftId).with(jwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(attachmentId));
        mockMvc.perform(get("/v1/spec-breakdown/drafts/{draftId}/attachments", otherDraftId).with(jwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(
                        delete(
                                        "/v1/spec-breakdown/drafts/{draftId}/attachments/{attachmentId}",
                                        draftId,
                                        attachmentId)
                                .with(jwt(userId)))
                .andExpect(status().isNoContent());
        verify(storage).delete(persisted.storageKey());
        org.junit.jupiter.api.Assertions.assertTrue(attachments.findActiveById(UUID.fromString(attachmentId)).isEmpty());
        mockMvc.perform(get("/v1/spec-breakdown/drafts/{draftId}/attachments", draftId).with(jwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsUnauthorizedDraftAccess() throws Exception {
        String draftId = createDraft(UUID.randomUUID().toString());
        mockMvc.perform(get("/v1/spec-breakdown/drafts/{draftId}/attachments", draftId).with(jwt(UUID.randomUUID().toString())))
                .andExpect(status().isForbidden());
    }

    private String createDraft(String userId) throws Exception {
        String payload =
                """
                {"projectId":"%s","title":"Draft","rawSpec":"Build it","candidateTree":"{\\\"nodes\\\":[]}"}
                """
                        .formatted(UUID.randomUUID());
        var response =
                mockMvc.perform(
                                post("/v1/spec-breakdown/drafts")
                                        .with(jwt(userId))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(payload))
                        .andExpect(status().isCreated())
                        .andReturn();
        return objectMapper.readTree(response.getResponse().getContentAsString()).get("id").asText();
    }
}
