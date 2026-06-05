package com.taskmind.backend.auth.application;

import java.util.UUID;

public interface AuthApplicationService {
    void signupEmail(SignupEmailCommand command);
    AuthTokens verifyOtp(VerifyOtpCommand command);
    AuthTokens login(LoginCommand command);
    AuthTokens refresh(RefreshTokenCommand command);
    void logout(LogoutCommand command);
    AuthUserView me(UUID authenticatedUserId);
}
