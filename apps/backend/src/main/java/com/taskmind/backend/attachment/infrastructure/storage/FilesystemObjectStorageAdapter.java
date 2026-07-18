package com.taskmind.backend.attachment.infrastructure.storage;

import com.taskmind.backend.attachment.domain.repository.ObjectStoragePort;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.InputStreamResource;

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
        try {
            Files.copy(
                    new SizeEnforcingInputStream(content, sizeBytes),
                    path,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(path);
            throw e;
        }
        contentTypes.put(key, contentType);
    }

    @Override
    public StoredObject get(String key) throws IOException {
        Path path = pathFor(key);
        return new StoredObject(
                new InputStreamResource(Files.newInputStream(path)),
                contentTypes.getOrDefault(key, "application/octet-stream"),
                Files.size(path));
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

    private static final class SizeEnforcingInputStream extends FilterInputStream {
        private final long maxBytes;
        private long bytesRead;

        private SizeEnforcingInputStream(InputStream in, long maxBytes) {
            super(in);
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value != -1) {
                recordBytes(1);
            }
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = super.read(b, off, len);
            if (read > 0) {
                recordBytes(read);
            }
            return read;
        }

        private void recordBytes(long count) throws IOException {
            bytesRead += count;
            if (bytesRead > maxBytes) {
                throw new IOException("Attachment stream exceeded declared size");
            }
        }
    }
}
