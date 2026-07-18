package com.taskmind.backend.attachment.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.util.StreamUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FilesystemObjectStorageAdapterTest {
    @TempDir Path tempDir;

    @Test
    void storesReadsAndDeletesObjects() throws Exception {
        FilesystemObjectStorageAdapter adapter = new FilesystemObjectStorageAdapter(tempDir);
        byte[] bytes = "hello".getBytes();
        adapter.put(
                "tasks/t1/file.txt", new ByteArrayInputStream(bytes), bytes.length, "text/plain");
        var object = adapter.get("tasks/t1/file.txt");
        assertArrayEquals(bytes, StreamUtils.copyToByteArray(object.resource().getInputStream()));
        assertEquals(bytes.length, object.sizeBytes());
        adapter.delete("tasks/t1/file.txt");
        assertThrows(
                java.nio.file.NoSuchFileException.class, () -> adapter.get("tasks/t1/file.txt"));
    }
    @Test
    void rejectsContentThatExceedsDeclaredSize() {
        FilesystemObjectStorageAdapter adapter = new FilesystemObjectStorageAdapter(tempDir);

        assertThrows(
                IOException.class,
                () ->
                        adapter.put(
                                "tasks/t1/too-large.txt",
                                new ByteArrayInputStream("too large".getBytes()),
                                3,
                                "text/plain"));
        assertFalse(Files.exists(tempDir.resolve("tasks/t1/too-large.txt")));
    }

    @Test
    void rejectsTraversalKeys() {
        FilesystemObjectStorageAdapter adapter = new FilesystemObjectStorageAdapter(tempDir);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        adapter.put(
                                "../x",
                                new ByteArrayInputStream(new byte[] {1}),
                                1,
                                "application/octet-stream"));
    }
}
