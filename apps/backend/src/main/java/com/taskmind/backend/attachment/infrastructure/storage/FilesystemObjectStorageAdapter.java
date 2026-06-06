package com.taskmind.backend.attachment.infrastructure.storage;

import com.taskmind.backend.attachment.domain.repository.ObjectStoragePort;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FilesystemObjectStorageAdapter implements ObjectStoragePort {
    private final Path root;
    private final Map<String, String> contentTypes = new ConcurrentHashMap<>();

    public FilesystemObjectStorageAdapter(Path root) {
        this.root = root;
    }

    @Override
    public void put(String key, InputStream content, long sizeBytes, String contentType)
            throws IOException {
        Path path = pathFor(key);
        Files.createDirectories(path.getParent());
        Files.copy(content, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        contentTypes.put(key, contentType);
    }

    @Override
    public StoredObject get(String key) throws IOException {
        Path path = pathFor(key);
        byte[] bytes = Files.readAllBytes(path);
        return new StoredObject(
                bytes, contentTypes.getOrDefault(key, "application/octet-stream"), bytes.length);
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(pathFor(key));
        contentTypes.remove(key);
    }

    private Path pathFor(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root.normalize())) {
            throw new IllegalArgumentException("Invalid object key");
        }
        return path;
    }
}
