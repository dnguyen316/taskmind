package com.taskmind.backend.security;

import com.taskmind.backend.auth.AuthenticatedUser;
import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class AuthenticatedUserResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(AuthenticatedUser.class);
    }

    @Override
    public AuthenticatedUser resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
        Principal principal = webRequest.getUserPrincipal();
        if (!(principal instanceof Authentication authentication)
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("An authenticated user is required");
        }

        return resolve(authentication);
    }

    public AuthenticatedUser resolve(Authentication authentication) {
        UUID userId = UUID.fromString(subject(authentication));
        Set<String> roles = new LinkedHashSet<>();
        Set<String> permissions = new LinkedHashSet<>();
        authentication.getAuthorities().forEach(authority -> {
            String value = authority.getAuthority();
            if (value != null && !value.isBlank()) {
                if (value.startsWith("ROLE_")) {
                    roles.add(value.substring("ROLE_".length()));
                } else if (!value.startsWith("SCOPE_")) {
                    permissions.add(value.startsWith("PERMISSION_") ? value.substring("PERMISSION_".length()) : value);
                }
            }
        });
        return new AuthenticatedUser(userId, Set.copyOf(roles), Set.copyOf(permissions));
    }

    private String subject(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return authentication.getName();
    }
}
