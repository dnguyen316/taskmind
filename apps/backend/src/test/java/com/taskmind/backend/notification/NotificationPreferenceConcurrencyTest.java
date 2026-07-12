package com.taskmind.backend.notification;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.notification.application.NotificationApplicationService;
import com.taskmind.backend.notification.domain.model.NotificationPreference;
import com.taskmind.backend.notification.domain.repository.NotificationPreferenceRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(com.taskmind.backend.security.TestJwtSupport.Config.class)
class NotificationPreferenceConcurrencyTest {
    @Autowired private NotificationPreferenceRepository preferences;
    @Autowired private NotificationApplicationService service;
    @Autowired private MockMvc mockMvc;

    @Test
    void concurrentFirstTimePreferenceWritesReturnSinglePreference() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first =
                    executor.submit(
                            () -> preferences.save(NotificationPreference.defaults(userId, now)));
            var second =
                    executor.submit(
                            () -> preferences.save(NotificationPreference.defaults(userId, now)));

            assertThat(first.get().userId()).isEqualTo(userId);
            assertThat(second.get().userId()).isEqualTo(userId);
            assertThat(preferences.findByUserId(userId)).isPresent().get().extracting(NotificationPreference::version).isEqualTo(0L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void stalePreferenceUpdateIsRejectedByService() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, Set.of("USER"));
        var current = service.preferences(user);

        service.updatePreferences(user, current.version(), false, true, false, null, null);

        assertThatThrownBy(
                        () ->
                                service.updatePreferences(
                                        user, current.version(), true, true, false, null, null))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void stalePreferenceUpdateReturnsConflict() throws Exception {
        String userId = UUID.randomUUID().toString();

        mockMvc.perform(get("/v1/notifications/preferences").with(jwt(userId))).andExpect(status().isOk());

        mockMvc.perform(
                        put("/v1/notifications/preferences")
                                .with(jwt(userId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"version":999,"inAppEnabled":true,"emailDigestEnabled":true,"slackEnabled":false}
                    """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrent update conflict"));
    }
}
