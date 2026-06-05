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
@Table(name = "otp_challenges")
public class OtpChallengeJpaEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserJpaEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthJpaEnums.OtpChannel channel;

    @Column(nullable = false, length = 320)
    private String destination;

    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OtpChallengeJpaEntity() {
    }

    public OtpChallengeJpaEntity(UUID id, UserJpaEntity user, AuthJpaEnums.OtpChannel channel, String destination,
            String otpHash, Instant expiresAt, Instant createdAt) {
        this.id = id; this.user = user; this.channel = channel; this.destination = destination;
        this.otpHash = otpHash; this.expiresAt = expiresAt; this.createdAt = createdAt;
    }

    public UserJpaEntity getUser() { return user; }
    public String getOtpHash() { return otpHash; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public void recordFailedAttempt() { this.attemptCount++; }
    public void consume(Instant now) { this.consumedAt = now; }
}
