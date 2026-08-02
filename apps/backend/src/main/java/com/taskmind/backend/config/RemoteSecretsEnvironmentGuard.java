package com.taskmind.backend.config;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class RemoteSecretsEnvironmentGuard implements InitializingBean {

    private static final List<String> REQUIRED_PROPERTIES =
            List.of(
                    "spring.datasource.url",
                    "spring.datasource.username",
                    "spring.datasource.password",
                    "taskmind.auth.jwt.secret",
                    "taskmind.auth.jwt.issuer",
                    "taskmind.auth.jwt.audience",
                    "taskmind.nova.service-token",
                    "taskmind.relay.client.service-token",
                    "taskmind.integrations.token-key");

    private final Environment environment;

    public RemoteSecretsEnvironmentGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        if (isLocalOrTest()) {
            return;
        }
        for (String property : REQUIRED_PROPERTIES) {
            String value = environment.getProperty(property);
            if (isUnsafe(value)) {
                throw new IllegalStateException(
                        property + " must be supplied with a non-development value for remote profiles");
            }
        }
    }

    private boolean isLocalOrTest() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("local") || profile.equals("test") || profile.equals("e2e"));
    }

    private boolean isUnsafe(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("localhost")
                || normalized.contains("development-only")
                || normalized.startsWith("local-")
                || normalized.equals("taskmind")
                || normalized.equals("password");
    }
}
