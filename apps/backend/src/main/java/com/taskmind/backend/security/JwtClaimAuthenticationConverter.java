package com.taskmind.backend.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtClaimAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        var authorities = extractAuthorities(jwt);
        var principalName = jwt.getSubject();
        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<String> authorities = new LinkedHashSet<>();

        authorities.addAll(readStringListClaim(jwt, "roles").stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .collect(Collectors.toSet()));

        authorities.addAll(readStringListClaim(jwt, "authorities").stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(authority -> !authority.isBlank())
                .collect(Collectors.toSet()));

        String scope = jwt.getClaimAsString("scope");
        if (scope != null && !scope.isBlank()) {
            for (String token : scope.split("\\s+")) {
                if (!token.isBlank()) {
                    authorities.add("SCOPE_" + token);
                }
            }
        }

        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    private List<String> readStringListClaim(Jwt jwt, String claimName) {
        var claim = jwt.getClaims().get(claimName);
        if (claim instanceof List<?> list) {
            return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        }
        return List.of();
    }
}
