package com.taskmind.backend.activity.search;

public class ActivitySearchDisabledException extends RuntimeException {
    public ActivitySearchDisabledException() {
        super("Activity search is not configured");
    }
}
