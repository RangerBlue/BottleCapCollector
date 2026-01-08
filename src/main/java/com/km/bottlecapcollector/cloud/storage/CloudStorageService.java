package com.km.bottlecapcollector.cloud.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.km.bottlecapcollector.cloud.storage.api.StorageImage;
import com.km.bottlecapcollector.cloud.storage.api.StorageService;
import com.km.bottlecapcollector.property.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for uploading and managing files in Google Cloud Storage.
 */
@Service
@Slf4j
public class CloudStorageService implements StorageService {

    private final Storage storage;
    private final String bucketName;
    private static final int SIGNED_URL_DURATION_MINUTES = 15;

    public CloudStorageService(Storage storage, AppProperties appProperties) {
        this.storage = storage;
        this.bucketName = appProperties.getCloudStorageBucketName();
    }

    /**
     * Uploads a file to Cloud Storage with userId/collectionKey folder structure.
     *
     * @param file the multipart file to upload
     * @param userId the user ID for the folder path
     * @param collectionKey the collection key for the folder path
     * @return StorageImage with storage metadata
     */
    @Override
    public StorageImage uploadImage(MultipartFile file, String userId, String collectionKey) {
        log.info("Uploading image for userId: {}, collectionKey: {}", userId, collectionKey);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String objectName = String.format("%s/%s/%s%s",
                userId,
                collectionKey,
                UUID.randomUUID(),
                extension);

        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        Blob blob = null;
        try {
            blob = storage.create(blobInfo, file.getBytes());
        } catch (IOException e) {
            throw new CloudStorageException(e.getMessage());
        }
        log.info("Successfully uploaded image to: gs://{}/{}", bucketName, objectName);

        return StorageImage.builder()
                .storageUrl(String.format("gs://%s/%s", bucketName, objectName))
                .bucketName(bucketName)
                .objectName(objectName)
                .contentType(contentType)
                .sizeBytes(file.getSize())
                .md5Hash(blob.getMd5())
                .uploadedAt(Instant.now())
                .build();
    }

    /**
     * Downloads an image from Cloud Storage and returns the bytes.
     *
     * @param objectName the object name to download
     * @return the image bytes
     */
    @Override
    public byte[] downloadImage(String objectName) {
        log.info("Downloading image: {}", objectName);
        BlobId blobId = BlobId.of(bucketName, objectName);
        Blob blob = storage.get(blobId);
        if (blob == null) {
            throw new CloudStorageException("Image not found: " + objectName);
        }
        return blob.getContent();
    }

    /**
     * Deletes an image from Cloud Storage.
     *
     * @param objectName the object name to delete
     * @return true if deleted, false if not found
     */
    @Override
    public boolean deleteImage(String objectName) {
        log.info("Deleting image: {}", objectName);
        BlobId blobId = BlobId.of(bucketName, objectName);
        boolean deleted = storage.delete(blobId);
        if (deleted) {
            log.info("Successfully deleted image: {}", objectName);
        } else {
            log.warn("Image not found for deletion: {}", objectName);
        }
        return deleted;
    }

    /**
     * Generates a signed URL for temporary access to an image.
     *
     * @param objectName the object name
     * @param durationMinutes how long the URL should be valid
     * @return the signed URL
     */
    @Override
    public String generateSignedUrl(String objectName) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build();
        return storage.signUrl(blobInfo, SIGNED_URL_DURATION_MINUTES, TimeUnit.MINUTES).toString();
    }


    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
