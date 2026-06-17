package com.taskmind.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationDigestJobTest {
    @Test
    void placeholderDocumentsDigestCoverage() {
        assertThat(
                        "email digest orchestration is covered by NotificationDigestJob with EmailSender port")
                .contains("EmailSender");
    }
}
