package com.taskmind.backend.auth.infrastructure.e2e;

import com.taskmind.backend.auth.domain.PasswordHasher;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "taskmind.auth.e2e-bypass.enabled", havingValue = "true")
public class E2eSuperAdminSeeder implements ApplicationRunner {
    private static final String EMAIL = "superadmin@taskmind.local";
    private final UserJpaRepository users;
    private final UserIdentityJpaRepository identities;
    private final RoleJpaRepository roles;
    private final UserRoleJpaRepository userRoles;
    private final PasswordHasher hasher;

    public E2eSuperAdminSeeder(
            UserJpaRepository users,
            UserIdentityJpaRepository identities,
            RoleJpaRepository roles,
            UserRoleJpaRepository userRoles,
            PasswordHasher hasher) {
        this.users = users;
        this.identities = identities;
        this.roles = roles;
        this.userRoles = userRoles;
        this.hasher = hasher;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.findByPrimaryEmail(EMAIL).isPresent()) {
            return;
        }
        Instant now = Instant.now();
        UserJpaEntity user =
                users.save(
                        new UserJpaEntity(
                                UUID.randomUUID(),
                                AuthJpaEnums.UserStatus.ACTIVE,
                                EMAIL,
                                hasher.hash("1"),
                                "TaskMind Super Admin",
                                now));
        UserIdentityJpaEntity identity =
                new UserIdentityJpaEntity(
                        UUID.randomUUID(), user, AuthJpaEnums.IdentityType.EMAIL, EMAIL, now);
        identity.verify(now);
        identities.save(identity);
        roles.findByName("ADMIN")
                .ifPresent(role -> userRoles.save(new UserRoleJpaEntity(user, role, now)));
    }
}
