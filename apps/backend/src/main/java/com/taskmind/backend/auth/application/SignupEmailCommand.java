package com.taskmind.backend.auth.application;

public record SignupEmailCommand(
    String email,
    String password,
    String displayName
) {
}
