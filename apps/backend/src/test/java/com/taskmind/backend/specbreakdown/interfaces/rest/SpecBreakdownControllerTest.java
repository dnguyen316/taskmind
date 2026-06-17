package com.taskmind.backend.specbreakdown.interfaces.rest;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.ai.NovaClient;
import com.taskmind.backend.ai.application.AiDomainEventPublisher;
import com.taskmind.backend.security.*;
import com.taskmind.backend.specbreakdown.application.*;
import com.taskmind.backend.specbreakdown.domain.model.*;
import com.taskmind.backend.specbreakdown.domain.repository.*;
import com.taskmind.backend.task.application.TaskApplicationService;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SpecBreakdownController.class)
@Import({SecurityConfig.class, AuthenticatedUserResolver.class, JwtClaimAuthenticationConverter.class, TestJwtSupport.Config.class, SpecBreakdownControllerTest.Fakes.class})
class SpecBreakdownControllerTest {
    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final UUID DRAFT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROJECT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    @Autowired MockMvc mockMvc;

    @Test void createsDraftBehindAuthenticatedCoreFacade() throws Exception { mockMvc.perform(post("/v1/spec-breakdown/drafts").with(jwt(USER_ID)).contentType(MediaType.APPLICATION_JSON).content("""
            {"projectId":"33333333-3333-3333-3333-333333333333","title":"Mobile spec","rawSpec":"Build mobile onboarding"}
            """)) .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(DRAFT_ID.toString())).andExpect(jsonPath("$.status").value("DRAFT")); }
    @Test void startsJobAndReturnsAcceptedStatusEnvelope() throws Exception { mockMvc.perform(post("/v1/spec-breakdown/drafts/{id}/jobs", DRAFT_ID).with(jwt(USER_ID)).contentType(MediaType.APPLICATION_JSON).content("{\"aiJobType\":\"OUTLINE\"}")) .andExpect(status().isAccepted()).andExpect(jsonPath("$.aiJobType").value("OUTLINE")); }
    @Test void returnsJobStatus() throws Exception { mockMvc.perform(get("/v1/spec-breakdown/jobs/{id}", Fakes.JOB_ID).with(jwt(USER_ID))) .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAUSED")); }

    @TestConfiguration static class Fakes {
        static final UUID JOB_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
        @Bean SpecBreakdownApplicationService specBreakdownApplicationService(){ return new SpecBreakdownApplicationService(new DraftRepo(), new JobRepo(), null, new ObjectMapper(), null, (u,t,p)->{}) {
            @Override public SpecBreakdownDraft createDraft(com.taskmind.backend.auth.AuthenticatedUser u, CreateDraftCommand c){ return draft(); }
            @Override public SpecBreakdownProcessingJob startJob(com.taskmind.backend.auth.AuthenticatedUser u, UUID draftId, SpecBreakdownJobType type){ return new SpecBreakdownProcessingJob(JOB_ID,0L,draftId,u.userId(),type,SpecBreakdownJobStatus.SUCCEEDED,"{}",null,null,false,false,Instant.now(),Instant.now(),Instant.now()); }
            @Override public Optional<SpecBreakdownProcessingJob> getJob(com.taskmind.backend.auth.AuthenticatedUser u, UUID id){ return Optional.of(new SpecBreakdownProcessingJob(id,0L,DRAFT_ID,u.userId(),SpecBreakdownJobType.BREAKDOWN,SpecBreakdownJobStatus.PAUSED,"{}",null,null,false,true,Instant.now(),Instant.now(),null)); }
        }; }
        private static SpecBreakdownDraft draft(){ Instant now=Instant.now(); return new SpecBreakdownDraft(DRAFT_ID,0L,PROJECT_ID,UUID.fromString(USER_ID),null,"Mobile spec","Build mobile onboarding",null,"{\"nodes\":[]}",SpecBreakdownStatus.DRAFT,null,null,null,null,null,null,now,now); }
    }
    static class DraftRepo implements SpecBreakdownDraftRepository { public SpecBreakdownDraft save(SpecBreakdownDraft d){return d;} public Optional<SpecBreakdownDraft> findById(UUID id){return Optional.empty();} public List<SpecBreakdownDraft> findByProjectId(UUID projectId){return List.of();} }
    static class JobRepo implements SpecBreakdownJobRepository { public SpecBreakdownProcessingJob save(SpecBreakdownProcessingJob j){return j;} public Optional<SpecBreakdownProcessingJob> findById(UUID id){return Optional.empty();} public List<SpecBreakdownProcessingJob> findByDraftId(UUID draftId){return List.of();} }
}
