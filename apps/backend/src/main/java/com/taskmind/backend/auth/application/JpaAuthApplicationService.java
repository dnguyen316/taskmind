package com.taskmind.backend.auth.application;

import com.taskmind.backend.auth.domain.AuthException;
import com.taskmind.backend.auth.domain.AuthFailureReason;
import com.taskmind.backend.auth.domain.OtpService;
import com.taskmind.backend.auth.domain.PasswordHasher;
import com.taskmind.backend.auth.domain.TokenService;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.AuthJpaEnums;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.RoleJpaRepository;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.SessionJpaEntity;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.SessionJpaRepository;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.UserIdentityJpaEntity;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.UserIdentityJpaRepository;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.UserJpaEntity;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.UserJpaRepository;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.UserRoleJpaEntity;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.UserRoleJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JpaAuthApplicationService implements AuthApplicationService {

    private static final String AUTH_FAILURE_MESSAGE = "Unable to process authentication request.";

    private final UserJpaRepository users;
    private final UserIdentityJpaRepository identities;
    private final RoleJpaRepository roles;
    private final UserRoleJpaRepository userRoles;
    private final SessionJpaRepository sessions;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;
    private final OtpService otpService;
    private final Duration refreshTtl;

    public JpaAuthApplicationService(
            UserJpaRepository users,
            UserIdentityJpaRepository identities,
            RoleJpaRepository roles,
            UserRoleJpaRepository userRoles,
            SessionJpaRepository sessions,
            PasswordHasher passwordHasher,
            TokenService tokenService,
            OtpService otpService,
            @Value("${taskmind.auth.jwt.refresh-ttl:P30D}") Duration refreshTtl) {
        this.users = users;
        this.identities = identities;
        this.roles = roles;
        this.userRoles = userRoles;
        this.sessions = sessions;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
        this.otpService = otpService;
        this.refreshTtl = refreshTtl;
    }

    @Override
    @Transactional
    public void signupEmail(SignupEmailCommand command) {
        String email = normalize(command.email());
        if (users.findByPrimaryEmail(email).isPresent()) {
            throw failure(AuthFailureReason.INVALID_CREDENTIALS);
        }

        Instant now = Instant.now();
        UserJpaEntity user =
                new UserJpaEntity(
                        UUID.randomUUID(),
                        AuthJpaEnums.UserStatus.PENDING_VERIFICATION,
                        email,
                        passwordHasher.hash(command.password()),
                        command.displayName().trim(),
                        now);
        users.save(user);
        identities.save(
                new UserIdentityJpaEntity(
                        UUID.randomUUID(), user, AuthJpaEnums.IdentityType.EMAIL, email, now));
        otpService.dispatchOtp("EMAIL", email);
    }

    @Override
    @Transactional
    public AuthTokens verifyOtp(VerifyOtpCommand command) {
        String email = normalize(command.email());
        UserIdentityJpaEntity identity =
                identities
                        .findByTypeAndValue(AuthJpaEnums.IdentityType.EMAIL, email)
                        .orElseThrow(() -> failure(AuthFailureReason.USER_NOT_FOUND));
        UserJpaEntity user = identity.getUser();
        if (user.getStatus() != AuthJpaEnums.UserStatus.PENDING_VERIFICATION
                || !otpService.verifyOtp(email, command.otp())) {
            throw failure(AuthFailureReason.INVALID_CREDENTIALS);
        }

        Instant now = Instant.now();
        identity.verify(now);
        user.activate(now);
        roles.findByName("MEMBER")
                .ifPresent(role -> userRoles.save(new UserRoleJpaEntity(user, role, now)));
        return issueSession(user);
    }

    @Override
    @Transactional
    public AuthTokens login(LoginCommand command) {
        UserJpaEntity user =
                users.findByPrimaryEmail(normalize(command.email()))
                        .orElseThrow(() -> failure(AuthFailureReason.INVALID_CREDENTIALS));
        if (user.getStatus() != AuthJpaEnums.UserStatus.ACTIVE
                || !passwordHasher.matches(command.password(), user.getPasswordHash())) {
            throw failure(AuthFailureReason.INVALID_CREDENTIALS);
        }

        user.recordLogin(Instant.now());
        return issueSession(user);
    }

    @Override
    @Transactional
    public AuthTokens refresh(RefreshTokenCommand command) {
        String refreshTokenHash = tokenService.hashRefreshToken(command.refreshToken());
        Instant now = Instant.now();
        SessionJpaEntity session =
                sessions.findLockedByRefreshTokenHash(refreshTokenHash)
                        .orElseThrow(() -> failure(AuthFailureReason.TOKEN_INVALID));
        if (!session.isUsableAt(now)) {
            throw failure(AuthFailureReason.TOKEN_EXPIRED);
        }

        UserJpaEntity user = session.getUser();
        AuthTokens tokens =
                tokenService.issue(user.getId(), user.getPrimaryEmail(), roleNames(user.getId()));
        session.rotate(tokenService.hashRefreshToken(tokens.refreshToken()), now.plus(refreshTtl));
        return tokens;
    }

    @Override
    @Transactional
    public void logout(LogoutCommand command) {
        sessions.findByRefreshTokenHash(tokenService.hashRefreshToken(command.refreshToken()))
                .ifPresent(session -> session.revoke(Instant.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthUserView me(UUID authenticatedUserId) {
        UserJpaEntity user =
                users.findById(authenticatedUserId)
                        .orElseThrow(() -> failure(AuthFailureReason.USER_NOT_FOUND));
        return new AuthUserView(user.getId(), user.getPrimaryEmail(), user.getDisplayName());
    }

    private AuthTokens issueSession(UserJpaEntity user) {
        AuthTokens tokens =
                tokenService.issue(user.getId(), user.getPrimaryEmail(), roleNames(user.getId()));
        Instant now = Instant.now();
        sessions.save(
                new SessionJpaEntity(
                        UUID.randomUUID(),
                        user,
                        tokenService.hashRefreshToken(tokens.refreshToken()),
                        now.plus(refreshTtl),
                        now));
        return tokens;
    }

    private Set<String> roleNames(UUID userId) {
        return userRoles.findByIdUserId(userId).stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toSet());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private AuthException failure(AuthFailureReason reason) {
        return new AuthException(reason, AUTH_FAILURE_MESSAGE);
    }
}
