package com.taskmind.backend.auth.infrastructure.otp;

import com.taskmind.backend.auth.domain.OtpService;
import com.taskmind.backend.auth.domain.PasswordHasher;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.AuthJpaEnums;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.OtpChallengeJpaEntity;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.OtpChallengeJpaRepository;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.UserJpaEntity;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.UserJpaRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JpaEmailOtpService implements OtpService {
    private final OtpChallengeJpaRepository challenges;
    private final UserJpaRepository users;
    private final PasswordHasher hasher;
    private final Duration ttl;
    private final String fixedCode;
    private final SecureRandom random = new SecureRandom();

    public JpaEmailOtpService(
            OtpChallengeJpaRepository challenges,
            UserJpaRepository users,
            PasswordHasher hasher,
            @Value("${taskmind.auth.otp.ttl:PT10M}") Duration ttl,
            @Value("${taskmind.auth.otp.fixed-code:}") String fixedCode) {
        this.challenges = challenges;
        this.users = users;
        this.hasher = hasher;
        this.ttl = ttl;
        this.fixedCode = fixedCode;
    }

    @Override
    @Transactional
    public void dispatchOtp(String channel, String destination) {
        UserJpaEntity user = users.findByPrimaryEmail(destination).orElseThrow();
        Instant now = Instant.now();
        String code = fixedCode.isBlank() ? "%06d".formatted(random.nextInt(1_000_000)) : fixedCode;
        challenges.save(
                new OtpChallengeJpaEntity(
                        UUID.randomUUID(),
                        user,
                        AuthJpaEnums.OtpChannel.valueOf(channel),
                        destination,
                        hasher.hash(code),
                        now.plus(ttl),
                        now));
    }

    @Override
    @Transactional
    public boolean verifyOtp(String destination, String otp) {
        OtpChallengeJpaEntity challenge =
                challenges
                        .findFirstByDestinationAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                                destination, Instant.now())
                        .orElse(null);
        if (challenge == null) return false;
        if (!hasher.matches(otp, challenge.getOtpHash())) {
            challenge.recordFailedAttempt();
            return false;
        }
        challenge.consume(Instant.now());
        return true;
    }
}
