package com.nuvi.online_renting.common.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * S3 storage service — all uploads go to a private bucket.
 * Public access is blocked at the bucket level.
 * Temporary access is granted via pre-signed GET URLs generated on each request.
 *
 * Key prefixes:
 *   items/   — item images        (pre-signed URLs valid 60 min by default)
 *   docs/    — KYC / seller docs  (pre-signed URLs valid 15 min by default)
 */
@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${app.s3.item-image-url-expiry-minutes:60}")
    private int itemImageExpiryMinutes;

    @Value("${app.s3.doc-url-expiry-minutes:15}")
    private int docUrlExpiryMinutes;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    // ─── Upload ────────────────────────────────────────────────────────────────

    /**
     * Uploads a file to S3 under the given key.
     * The key is stored in the database — NOT a public URL.
     * Use {@link #generatePresignedUrl} to serve the file to clients.
     */
    public void uploadFile(String key, MultipartFile file) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.debug("Uploaded to S3: {}", key);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Uploads a list of KYC/seller documents to S3 under the docs/ prefix.
     * Returns the list of S3 keys (not URLs) to be persisted in the database.
     */
    public List<String> uploadDocs(Long applicationId, List<MultipartFile> files) {
        List<String> keys = new ArrayList<>();
        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                    : "doc";
            String key = "docs/seller_" + applicationId + "_" + UUID.randomUUID() + "_" + originalName;
            uploadFile(key, file);
            keys.add(key);
        }
        return keys;
    }

    // ─── Pre-signed URLs ───────────────────────────────────────────────────────

    /**
     * Generates a temporary pre-signed GET URL for the given S3 key.
     * The URL is valid for {@code durationMinutes} and then expires automatically.
     *
     * @param key             S3 object key (e.g. "items/item_1_123.jpg")
     * @param durationMinutes How long the URL is valid
     * @return A time-limited HTTPS URL clients can use to access the file
     */
    public String generatePresignedUrl(String key, int durationMinutes) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(durationMinutes))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build())
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * Convenience method — generates a pre-signed URL for an item image (60 min).
     * Returns null if the key is null or blank (item has no image yet).
     */
    public String generateItemImageUrl(String key) {
        if (key == null || key.isBlank()) return null;
        return generatePresignedUrl(key, itemImageExpiryMinutes);
    }

    /**
     * Convenience method — generates a pre-signed URL for a private document (15 min).
     * Returns null if the key is null or blank.
     */
    public String generateDocUrl(String key) {
        if (key == null || key.isBlank()) return null;
        return generatePresignedUrl(key, docUrlExpiryMinutes);
    }

    // ─── Delete ────────────────────────────────────────────────────────────────

    /**
     * Deletes a file from S3 by its key.
     */
    public void deleteFile(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
        log.debug("Deleted from S3: {}", key);
    }
}
