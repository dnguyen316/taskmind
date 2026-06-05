package com.taskmind.backend.auth.application;

public record LoginCommand(
    String email,
    String password
) {
}
