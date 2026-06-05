package com.taskmind.backend.auth.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_identities")
public class UserIdentityJpaEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthJpaEnums.IdentityType type;

    @Column(nullable = false, length = 320)
    private String value;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserIdentityJpaEntity() {
    }

    public UserIdentityJpaEntity(UUID id, UserJpaEntity user, AuthJpaEnums.IdentityType type, String value, Instant now) {
        this.id = id; this.user = user; this.type = type; this.value = value; this.createdAt = now;
    }

    public UserJpaEntity getUser() { return user; }
    public boolean isVerified() { return verified; }
    public void verify(Instant now) { this.verified = true; this.verifiedAt = now; }
}
