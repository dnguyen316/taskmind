package com.taskmind.backend.auth.infrastructure.e2e;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AuthE2eBypassGuard {
    private static final Set<String> ALLOWED_BYPASS_PROFILES = Set.of("local", "test", "e2e");

    private final Environment environment;
    private final boolean enabled;

    public AuthE2eBypassGuard(
            Environment environment, @Value("${taskmind.auth.e2e-bypass.enabled:false}") boolean enabled) {
        this.environment = environment;
        this.enabled = enabled;
    }

    @PostConstruct
    void validate() {
        if (!enabled) {
            return;
        }

        java.util.List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.stream().noneMatch(ALLOWED_BYPASS_PROFILES::contains)) {
            throw new IllegalStateException(
                    "E2E authentication bypass may only be enabled with local, test, or e2e profiles");
        }
    }
}
