package com.taskmind.backend.attachment.domain.repository;

import java.io.IOException;
import java.io.InputStream;

public interface ObjectStoragePort {
    void put(String key, InputStream content, long sizeBytes, String contentType)
            throws IOException;

    StoredObject get(String key) throws IOException;

    void delete(String key) throws IOException;

    record StoredObject(byte[] bytes, String contentType, long sizeBytes) {}
}
