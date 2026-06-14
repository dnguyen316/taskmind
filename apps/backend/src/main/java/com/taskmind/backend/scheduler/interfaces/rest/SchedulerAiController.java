package com.taskmind.backend.scheduler.interfaces.rest;

import com.taskmind.backend.ai.application.AiFacadeApplicationService;
import com.taskmind.backend.ai.application.DurationEstimateResult;
import com.taskmind.backend.ai.application.RationalePhraseResult;
import com.taskmind.backend.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/scheduler/ai")
public class SchedulerAiController {
    private final AiFacadeApplicationService aiFacade;

    public SchedulerAiController(AiFacadeApplicationService aiFacade) {
        this.aiFacade = aiFacade;
    }

    @PostMapping("/duration-estimate")
    public DurationEstimateResponse durationEstimate(
            AuthenticatedUser requester, @Valid @RequestBody DurationEstimateRequest request) {
        DurationEstimateResult result =
                aiFacade.durationEstimate(requester.userId(), request.title(), request.description());
        return new DurationEstimateResponse(result.durationMinutes(), result.rationale(), result.confidence());
    }

    @PostMapping("/rationale-phrase")
    public RationalePhraseResponse rationalePhrase(
            AuthenticatedUser requester, @Valid @RequestBody RationalePhraseRequest request) {
        RationalePhraseResult result =
                aiFacade.rationalePhrase(requester.userId(), request.title(), request.context());
        return new RationalePhraseResponse(request.blockId(), result.rationale());
    }

    public record DurationEstimateRequest(@NotBlank String title, String description) {}

    public record DurationEstimateResponse(int durationMinutes, String rationale, double confidence) {}

    public record RationalePhraseRequest(UUID blockId, @NotBlank String title, String context) {}

    public record RationalePhraseResponse(UUID blockId, String rationale) {}
}
