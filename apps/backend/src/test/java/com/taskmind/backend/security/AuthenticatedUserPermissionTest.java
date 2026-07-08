package com.taskmind.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.taskmind.backend.auth.AuthenticatedUser;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AuthenticatedUserPermissionTest {

    @Test
    void authenticatedUserSupportsPermissionChecks() {
        AuthenticatedUser user =
                new AuthenticatedUser(
                        UUID.randomUUID(), Set.of("MEMBER"), Set.of("project.read"));

        assertThat(user.hasPermission("PROJECT.READ")).isTrue();
        assertThat(user.hasAnyPermission("team.manage", "project.read")).isTrue();
        assertThat(user.hasPermission("rbac.roles.manage")).isFalse();
    }

    @Test
    void converterAndResolverExposeJwtPermissionsToApplicationCode() {
        UUID userId = UUID.randomUUID();
        Jwt jwt =
                new Jwt(
                        "token",
                        Instant.now(),
                        Instant.now().plusSeconds(300),
                        Map.of("alg", "none"),
                        Map.of(
                                "sub", userId.toString(),
                                "roles", List.of("MANAGER"),
                                "permissions", List.of("project.update", "project.members.manage")));

        var authentication = new JwtClaimAuthenticationConverter().convert(jwt);
        AuthenticatedUser user = new AuthenticatedUserResolver().resolve(authentication);

        assertThat(user.userId()).isEqualTo(userId);
        assertThat(user.roles()).containsExactly("MANAGER");
        assertThat(user.permissions()).containsExactlyInAnyOrder("project.update", "project.members.manage");
        assertThat(user.hasPermission("project.update")).isTrue();
    }
}
