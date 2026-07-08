package com.taskmind.backend.auth.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.RoleJpaEntity;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.RoleJpaRepository;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.UserJpaEntity;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.UserJpaRepository;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.UserRoleJpaEntity;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.UserRoleJpaRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GlobalRoleAssignmentService {
    private final UserJpaRepository users;
    private final RoleJpaRepository roles;
    private final UserRoleJpaRepository userRoles;

    public GlobalRoleAssignmentService(
            UserJpaRepository users, RoleJpaRepository roles, UserRoleJpaRepository userRoles) {
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
    }

    @Transactional
    public String changeRole(AuthenticatedUser actor, UUID userId, String roleName) {
        if (!actor.hasPermission("rbac.roles.manage")) {
            throw new SecurityException("Global role assignment requires rbac.roles.manage");
        }
        UserJpaEntity user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        String normalizedRole = roleName.trim().toUpperCase(Locale.ROOT);
        RoleJpaEntity role = roles.findByName(normalizedRole).orElseThrow(() -> new IllegalArgumentException("Role not found"));
        userRoles.deleteByIdUserId(userId);
        userRoles.save(new UserRoleJpaEntity(user, role, Instant.now()));
        return normalizedRole;
    }
}
