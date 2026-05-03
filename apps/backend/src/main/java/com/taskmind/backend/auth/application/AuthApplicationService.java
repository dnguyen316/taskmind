package com.taskmind.backend.auth.application;

public interface AuthApplicationService {
    AuthTokens signupEmail(SignupEmailCommand command);

    AuthTokens login(LoginCommand command);

    AuthTokens refresh(RefreshTokenCommand command);

    void logout(LogoutCommand command);

    AuthUserView me(String authorizationHeader);
}
