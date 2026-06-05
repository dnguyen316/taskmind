package com.taskmind.backend.auth.application;
public record VerifyOtpCommand(String email, String otp) { }
