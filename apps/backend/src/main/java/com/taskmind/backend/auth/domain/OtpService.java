package com.taskmind.backend.auth.domain;

public interface OtpService {
    void dispatchOtp(String channel, String destination);

    boolean verifyOtp(String destination, String otp);
}
