package com.taskmind.backend.attachment.infrastructure.storage;

import com.taskmind.backend.attachment.config.AttachmentStorageProperties;
import com.taskmind.backend.attachment.domain.repository.ObjectStoragePort;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class S3ObjectStorageAdapter implements ObjectStoragePort {
    private final S3Client client;
    private final String bucket;

    public S3ObjectStorageAdapter(AttachmentStorageProperties properties) {
        this(createClient(properties), properties.getBucket());
    }

    public S3ObjectStorageAdapter(S3Client client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void put(String key, InputStream content, long sizeBytes, String contentType)
            throws IOException {
        client.putObject(
                builder -> builder.bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromInputStream(content, sizeBytes));
    }

    @Override
    public StoredObject get(String key) throws IOException {
        ResponseBytes<GetObjectResponse> response =
                client.getObjectAsBytes(builder -> builder.bucket(bucket).key(key).build());
        return new StoredObject(
                response.asByteArray(),
                response.response().contentType(),
                response.response().contentLength());
    }

    @Override
    public void delete(String key) throws IOException {
        client.deleteObject(builder -> builder.bucket(bucket).key(key).build());
    }

    private static S3Client createClient(AttachmentStorageProperties properties) {
        S3ClientBuilder builder =
                S3Client.builder()
                        .region(Region.of(properties.getS3Region()))
                        .forcePathStyle(properties.isS3PathStyleAccess());
        if (properties.getS3Endpoint() != null && !properties.getS3Endpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getS3Endpoint()));
        }
        return builder.build();
    }
}
