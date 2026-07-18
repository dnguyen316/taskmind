package com.taskmind.backend.onboarding.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.onboarding.application.OnboardingApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/onboarding")
public class OnboardingController {
    private final OnboardingApplicationService onboarding;

    public OnboardingController(OnboardingApplicationService onboarding) {
        this.onboarding = onboarding;
    }

    @GetMapping("/templates")
    public List<OnboardingApplicationService.TemplateSummary> templates() {
        return onboarding.templates();
    }

    @PostMapping("/complete")
    public OnboardingApplicationService.OnboardingResult complete(
            AuthenticatedUser user, @Valid @RequestBody CompleteOnboardingRequest request) {
        return onboarding.complete(
                user,
                request.workspaceType(),
                request.planningStyle(),
                request.startMode(),
                request.templateKey());
    }

    @PostMapping("/demo/reset")
    public OnboardingApplicationService.OnboardingResult resetDemo(AuthenticatedUser user) {
        try {
            return onboarding.resetDemoWorkspace(user);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The requested resource was not found.", e);
        }
    }

    public record CompleteOnboardingRequest(
            @NotBlank String workspaceType,
            @NotBlank String planningStyle,
            @NotBlank String startMode,
            String templateKey) {}
}
