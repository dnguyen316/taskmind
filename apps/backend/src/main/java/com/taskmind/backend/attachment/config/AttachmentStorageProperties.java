package com.taskmind.backend.attachment.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "taskmind.attachments")
public class AttachmentStorageProperties {
    private String adapter = "filesystem";
    private long maxSizeBytes = 10 * 1024 * 1024;
    private Path filesystemRoot = Path.of("build/taskmind-attachments");
    private String bucket = "taskmind-attachments";
    private String s3Endpoint;
    private String s3Region = "ap-southeast-2";
    private boolean s3PathStyleAccess = true;

    public String getAdapter() {
        return adapter;
    }

    public void setAdapter(String adapter) {
        this.adapter = adapter;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public Path getFilesystemRoot() {
        return filesystemRoot;
    }

    public void setFilesystemRoot(Path filesystemRoot) {
        this.filesystemRoot = filesystemRoot;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getS3Endpoint() {
        return s3Endpoint;
    }

    public void setS3Endpoint(String s3Endpoint) {
        this.s3Endpoint = s3Endpoint;
    }

    public String getS3Region() {
        return s3Region;
    }

    public void setS3Region(String s3Region) {
        this.s3Region = s3Region;
    }

    public boolean isS3PathStyleAccess() {
        return s3PathStyleAccess;
    }

    public void setS3PathStyleAccess(boolean s3PathStyleAccess) {
        this.s3PathStyleAccess = s3PathStyleAccess;
    }
}
