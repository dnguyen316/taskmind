package com.taskmind.backend.auth.infrastructure.persistence.jpa;

public final class AuthJpaEnums {

    private AuthJpaEnums() {}

    public enum UserStatus {
        PENDING_VERIFICATION,
        ACTIVE,
        LOCKED,
        DISABLED
    }

    public enum IdentityType {
        EMAIL,
        PHONE
    }

    public enum OtpChannel {
        EMAIL,
        SMS
    }
}
