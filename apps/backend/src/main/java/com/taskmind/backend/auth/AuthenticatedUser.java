package com.taskmind.backend.auth;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(UUID userId, Set<String> roles) {

    public boolean hasRole(String role) {
        return roles.stream().anyMatch(existing -> existing.equalsIgnoreCase(role));
    }

    public boolean isPrivileged() {
        return hasRole("ADMIN") || hasRole("MANAGER");
    }
}
