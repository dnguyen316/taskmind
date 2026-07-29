package com.taskmind.ai.security;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DevelopmentCredentialStartupGuard {
    private static final Set<String> ALLOWED_DEVELOPMENT_PROFILES = Set.of("local", "test", "e2e");
    private static final Map<String, String> DEVELOPMENT_CREDENTIALS = Map.of(
            "taskmind.ai.service-token",
            "development-only-nova-service-token",
            "taskmind.core.service-token",
            "local-core-token",
            "taskmind.relay.service-token",
            "local-relay-token");

    private final Environment environment;
    private final String aiServiceToken;
    private final String coreServiceToken;
    private final String relayServiceToken;

    public DevelopmentCredentialStartupGuard(
            Environment environment,
            @Value("${taskmind.ai.service-token:}") String aiServiceToken,
            @Value("${taskmind.core.service-token:}") String coreServiceToken,
            @Value("${taskmind.relay.service-token:}") String relayServiceToken) {
        this.environment = environment;
        this.aiServiceToken = aiServiceToken;
        this.coreServiceToken = coreServiceToken;
        this.relayServiceToken = relayServiceToken;
    }

    @PostConstruct
    void validate() {
        if (usesOnlyAllowedDevelopmentProfiles()) {
            return;
        }
        rejectIfDevelopmentCredential("taskmind.ai.service-token", aiServiceToken);
        rejectIfDevelopmentCredential("taskmind.core.service-token", coreServiceToken);
        rejectIfDevelopmentCredential("taskmind.relay.service-token", relayServiceToken);
    }

    private boolean usesOnlyAllowedDevelopmentProfiles() {
        java.util.List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        return activeProfiles.size() == 1 && ALLOWED_DEVELOPMENT_PROFILES.contains(activeProfiles.get(0));
    }

    private void rejectIfDevelopmentCredential(String propertyName, String value) {
        if (DEVELOPMENT_CREDENTIALS.get(propertyName).equals(value)) {
            throw new IllegalStateException(propertyName + " must be changed for non-local/non-test deployments");
        }
    }
}
