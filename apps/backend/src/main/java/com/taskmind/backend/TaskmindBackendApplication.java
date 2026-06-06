package com.taskmind.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TaskmindBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskmindBackendApplication.class, args);
    }
}
