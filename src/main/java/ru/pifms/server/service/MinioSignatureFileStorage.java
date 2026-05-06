package ru.pifms.server.service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import ru.pifms.server.config.MinioProperties;

@Service
public class MinioSignatureFileStorage {

    private static final String OBJECT_PREFIX = "signatures/";
    private static final String FALLBACK_FILE_NAME = "uploaded-file";

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioSignatureFileStorage(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @PostConstruct
    public void validateBucket() {
        if (!properties.enabled()) {
            return;
        }

        try {
            boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(properties.bucket())
                    .build()
            );
            if (!bucketExists) {
                throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "MinIO bucket " + properties.bucket() + " does not exist"
                );
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to validate MinIO bucket", ex);
        }
    }

    public StoredSignatureFile upload(MultipartFile file) {
        ensureStorageEnabled();
        String originalFileName = normalizeFileName(file.getOriginalFilename());
        String objectKey = OBJECT_PREFIX + UUID.randomUUID() + "/" + originalFileName;
        String contentType = resolveContentType(file);

        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType)
                    .build()
            );
            return new StoredSignatureFile(objectKey, originalFileName, file.getSize(), contentType);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "failed to upload file to MinIO", ex);
        }
    }

    public PresignedSignatureFileUrl createPresignedUrl(UUID signatureId, String objectKey, String originalFileName) {
        ensureStorageEnabled();
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .build()
            );
            String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .expiry(properties.presignedUrlTtlSeconds(), TimeUnit.SECONDS)
                    .build()
            );
            Instant expiresAt = Instant.now().plusSeconds(properties.presignedUrlTtlSeconds());
            return new PresignedSignatureFileUrl(signatureId, originalFileName, url, expiresAt);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "failed to create pre-signed URL", ex);
        }
    }

    private void ensureStorageEnabled() {
        if (!properties.enabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "MinIO storage is disabled");
        }
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return FALLBACK_FILE_NAME;
        }

        String normalized = fileName.trim().replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash >= 0) {
            normalized = normalized.substring(lastSlash + 1);
        }

        normalized = normalized.replaceAll("[^A-Za-z0-9._-]", "_");
        if (normalized.isBlank() || normalized.equals(".") || normalized.equals("..")) {
            return FALLBACK_FILE_NAME;
        }
        return normalized;
    }

    private String resolveContentType(MultipartFile file) {
        if (file.getContentType() == null || file.getContentType().isBlank()) {
            return "application/octet-stream";
        }
        return file.getContentType().trim();
    }

    public record StoredSignatureFile(
        String objectKey,
        String originalFileName,
        long size,
        String contentType
    ) {
    }

    public record PresignedSignatureFileUrl(
        UUID signatureId,
        String originalFileName,
        String url,
        Instant expiresAt
    ) {
    }
}
