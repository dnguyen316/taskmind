package com.taskmind.relay.security;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

final class ServiceAuthenticationToken extends AbstractAuthenticationToken {
    private static final String PRINCIPAL = "taskmind-service";
    private static final String PROTECTED_CREDENTIALS = "[PROTECTED]";

    ServiceAuthenticationToken() {
        super(List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return PROTECTED_CREDENTIALS;
    }

    @Override
    public Object getPrincipal() {
        return PRINCIPAL;
    }
}
