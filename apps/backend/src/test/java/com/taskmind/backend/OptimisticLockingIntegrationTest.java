package com.taskmind.backend;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.taskmind.backend.project.domain.model.Project;
import com.taskmind.backend.project.infrastructure.persistence.jpa.JpaProjectRepository;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskSource;
import com.taskmind.backend.task.domain.model.TaskStatus;
import com.taskmind.backend.task.infrastructure.persistence.jpa.JpaTaskRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OptimisticLockingIntegrationTest {

    @Autowired
    private JpaTaskRepository taskRepository;

    @Autowired
    private JpaProjectRepository projectRepository;

    @Test
    void rejectsStaleTaskUpdate() {
        var now = Instant.now();
        var created = taskRepository.save(new Task(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            null,
            "Concurrent task",
            null,
            TaskStatus.TODO,
            2,
            null,
            null,
            null,
            TaskSource.MANUAL,
            null,
            now,
            now
        ));

        var firstSnapshot = taskRepository.findById(created.id()).orElseThrow();
        var staleSnapshot = taskRepository.findById(created.id()).orElseThrow();

        taskRepository.save(firstSnapshot.withStatus(TaskStatus.IN_PROGRESS, Instant.now()));

        assertThrows(ObjectOptimisticLockingFailureException.class,
            () -> taskRepository.save(staleSnapshot.withStatus(TaskStatus.DONE, Instant.now())));
    }

    @Test
    void rejectsStaleProjectUpdate() {
        var now = Instant.now();
        var projectKey = "CP" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        var created = projectRepository.save(new Project(
            UUID.randomUUID(),
            null,
            "Concurrency project",
            projectKey,
            null,
            UUID.randomUUID(),
            null,
            now,
            now
        ));

        var firstSnapshot = projectRepository.findById(created.id()).orElseThrow();
        var staleSnapshot = projectRepository.findById(created.id()).orElseThrow();

        projectRepository.save(new Project(
            firstSnapshot.id(),
            firstSnapshot.version(),
            "Concurrency project v2",
            firstSnapshot.key(),
            firstSnapshot.description(),
            firstSnapshot.ownerUserId(),
            firstSnapshot.archivedAt(),
            firstSnapshot.createdAt(),
            Instant.now()
        ));

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> projectRepository.save(new Project(
            staleSnapshot.id(),
            staleSnapshot.version(),
            "Concurrency project stale",
            staleSnapshot.key(),
            staleSnapshot.description(),
            staleSnapshot.ownerUserId(),
            staleSnapshot.archivedAt(),
            staleSnapshot.createdAt(),
            Instant.now()
        )));
    }
}
