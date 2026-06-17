package com.taskmind.backend.integration.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.integration.domain.model.*;
import com.taskmind.backend.integration.domain.repository.IntegrationConnectionRepository;
import com.taskmind.backend.integration.infrastructure.security.TokenCipher;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IntegrationConnectionApplicationService {
    private final IntegrationConnectionRepository connections;
    private final TokenCipher cipher;
    public IntegrationConnectionApplicationService(IntegrationConnectionRepository connections, TokenCipher cipher) { this.connections = connections; this.cipher = cipher; }
    @Transactional
    public IntegrationConnection connect(AuthenticatedUser actor, IntegrationProvider provider, String accountName, String baseUrl, String externalId, String accessToken, String refreshToken, String scopes) {
        if (accountName == null || accountName.isBlank() || accessToken == null || accessToken.isBlank()) throw new IllegalArgumentException("accountName and accessToken are required");
        Instant now = Instant.now();
        return connections.save(new IntegrationConnection(UUID.randomUUID(), null, provider, accountName.trim(), baseUrl, externalId, actor.userId(), cipher.encrypt(accessToken), cipher.encrypt(refreshToken), null, scopes, "CONNECTED", now, now));
    }
    public List<IntegrationConnection> list(AuthenticatedUser actor) { return connections.findByOwnerUserId(actor.userId()); }
    public IntegrationConnection requireOwned(AuthenticatedUser actor, UUID id) { return connections.findById(id).filter(c -> actor.isPrivileged() || c.ownerUserId().equals(actor.userId())).orElseThrow(() -> new IllegalArgumentException("Connection not found")); }
}
