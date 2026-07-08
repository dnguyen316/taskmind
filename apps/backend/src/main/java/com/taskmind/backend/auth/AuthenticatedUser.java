package com.taskmind.backend.auth;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(UUID userId, Set<String> roles, Set<String> permissions) {

    public AuthenticatedUser {
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
    }

    public AuthenticatedUser(UUID userId, Set<String> roles) {
        this(userId, roles, Set.of());
    }

    public boolean hasRole(String role) {
        return roles.stream().anyMatch(existing -> existing.equalsIgnoreCase(role));
    }

    public boolean hasPermission(String permission) {
        return permissions.stream().anyMatch(existing -> existing.equalsIgnoreCase(permission));
    }

    public boolean hasAnyPermission(String... candidates) {
        for (String candidate : candidates) {
            if (hasPermission(candidate)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPrivileged() {
        return hasRole("ADMIN") || hasRole("MANAGER");
    }
}
