package com.taskmind.backend.security;

import java.util.Arrays;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieSecurityPropertiesGuard implements InitializingBean {
    private final Environment environment;
    private final boolean secureCookies;

    public RefreshCookieSecurityPropertiesGuard(
            Environment environment, @Value("${taskmind.auth.cookies.secure:false}") boolean secureCookies) {
        this.environment = environment;
        this.secureCookies = secureCookies;
    }

    @Override
    public void afterPropertiesSet() {
        if (!secureCookies && requiresSecureCookies()) {
            throw new IllegalStateException(
                    "taskmind.auth.cookies.secure must be true outside local/test profiles.");
        }
    }

    private boolean requiresSecureCookies() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length > 0
                && Arrays.stream(activeProfiles)
                        .noneMatch(profile -> profile.equals("local") || profile.equals("test"));
    }
}
