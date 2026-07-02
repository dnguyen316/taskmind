package com.taskmind.backend.auth.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuthJpaEnums.UserStatus status;

    @Column(name = "primary_email", length = 320)
    private String primaryEmail;

    @Column(name = "primary_phone", length = 20)
    private String primaryPhone;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "display_name", length = 80)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;

    @Column(name = "onboarding_workspace_type", length = 40)
    private String onboardingWorkspaceType;

    @Column(name = "onboarding_planning_style", length = 40)
    private String onboardingPlanningStyle;

    protected UserJpaEntity() {
    }

    public UserJpaEntity(UUID id, AuthJpaEnums.UserStatus status, String primaryEmail, String passwordHash,
            String displayName, Instant now) {
        this.id = id;
        this.status = status;
        this.primaryEmail = primaryEmail;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public AuthJpaEnums.UserStatus getStatus() { return status; }
    public String getPrimaryEmail() { return primaryEmail; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public boolean isOnboardingCompleted() { return onboardingCompleted; }
    public String getOnboardingWorkspaceType() { return onboardingWorkspaceType; }
    public String getOnboardingPlanningStyle() { return onboardingPlanningStyle; }
    public void completeOnboarding(String workspaceType, String planningStyle, Instant now) { this.onboardingCompleted = true; this.onboardingWorkspaceType = workspaceType; this.onboardingPlanningStyle = planningStyle; this.updatedAt = now; }
    public void resetOnboarding(Instant now) { this.onboardingCompleted = false; this.onboardingWorkspaceType = null; this.onboardingPlanningStyle = null; this.updatedAt = now; }
    public void activate(Instant now) { this.status = AuthJpaEnums.UserStatus.ACTIVE; this.updatedAt = now; }
    public void recordLogin(Instant now) { this.lastLoginAt = now; this.updatedAt = now; }
}
