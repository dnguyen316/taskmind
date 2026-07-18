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
    private final boolean dangerousLocalSeedAllowed;

    public AuthE2eBypassGuard(
            Environment environment,
            @Value("${taskmind.auth.e2e-bypass.enabled:false}") boolean enabled,
            @Value("${taskmind.auth.e2e-bypass.allow-dangerous-local-seed:false}") boolean dangerousLocalSeedAllowed) {
        this.environment = environment;
        this.enabled = enabled;
        this.dangerousLocalSeedAllowed = dangerousLocalSeedAllowed;
    }

    @PostConstruct
    void validate() {
        if (!enabled) {
            return;
        }

        if (!dangerousLocalSeedAllowed) {
            throw new IllegalStateException(
                    "E2E authentication bypass requires taskmind.auth.e2e-bypass.allow-dangerous-local-seed=true");
        }

        java.util.List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.size() != 1 || !ALLOWED_BYPASS_PROFILES.contains(activeProfiles.get(0))) {
            throw new IllegalStateException(
                    "E2E authentication bypass may only be enabled with exactly one of the local, test, or e2e profiles");
        }
    }
}
