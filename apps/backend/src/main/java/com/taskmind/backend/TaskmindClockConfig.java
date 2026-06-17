package com.taskmind.backend;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskmindClockConfig {
    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
