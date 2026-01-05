package com.km.bottlecapcollector.firestore.service;

import com.google.cloud.storage.*;
import com.km.bottlecapcollector.firestore.document.FirestoreImage;
import com.km.bottlecapcollector.property.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/**
 * Service for uploading and managing files in Google Cloud Storage.
 */
@Service
@Slf4j
public class CloudStorageService {

    private static final String TEMPORARY_FOLDER = "temporary";
    private static final String PERMANENT_FOLDER = "bottle-caps";

    private final Storage storage;
    private final String bucketName;

    public CloudStorageService(Storage storage, AppProperties appProperties) {
        this.storage = storage;
        this.bucketName = appProperties.getCloudStorageBucketName();
    }

    /**
     * Uploads a file to Cloud Storage and returns image metadata.
     *
     * @param file the multipart file to upload
     * @param bottleCapId the bottle cap ID to associate with the image
     * @return FirestoreImage with storage metadata
     * @throws IOException if upload fails
     */
    public FirestoreImage uploadImage(MultipartFile file, String bottleCapId) throws IOException {
        return uploadImage(file, bottleCapId, false);
    }

    /**
     * Uploads a file to Cloud Storage with temporary/permanent folder distinction.
     *
     * @param file the multipart file to upload
     * @param bottleCapId the bottle cap ID to associate with the image
     * @param temporary if true, uploads to temporary folder
     * @return FirestoreImage with storage metadata
     * @throws IOException if upload fails
     */
    public FirestoreImage uploadImage(MultipartFile file, String bottleCapId, boolean temporary) throws IOException {
        log.info("Uploading {} image for bottle cap: {}", temporary ? "temporary" : "permanent", bottleCapId);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String folder = temporary ? TEMPORARY_FOLDER : PERMANENT_FOLDER;
        String objectName = String.format("%s/%s/%s%s",
                folder,
                bottleCapId,
                UUID.randomUUID().toString(),
                extension);

        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        Blob blob = storage.create(blobInfo, file.getBytes());

        log.info("Successfully uploaded image to: gs://{}/{}", bucketName, objectName);

        return FirestoreImage.builder()
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
    public byte[] downloadImage(String objectName) {
        log.info("Downloading image: {}", objectName);
        BlobId blobId = BlobId.of(bucketName, objectName);
        Blob blob = storage.get(blobId);
        if (blob == null) {
            throw new RuntimeException("Image not found: " + objectName);
        }
        return blob.getContent();
    }

    /**
     * Deletes an image from Cloud Storage.
     *
     * @param objectName the object name to delete
     * @return true if deleted, false if not found
     */
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
    public String generateSignedUrl(String objectName, int durationMinutes) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build();
        return storage.signUrl(blobInfo, durationMinutes, java.util.concurrent.TimeUnit.MINUTES).toString();
    }

    /**
     * Moves an image from temporary to permanent folder.
     *
     * @param currentObjectName the current object name in temporary folder
     * @param bottleCapId the bottle cap ID
     * @return the new FirestoreImage with updated storage metadata
     */
    public FirestoreImage moveToPermament(String currentObjectName, String bottleCapId) {
        log.info("Moving image from temporary to permanent: {}", currentObjectName);

        if (!currentObjectName.startsWith(TEMPORARY_FOLDER + "/")) {
            log.warn("Image is not in temporary folder, skipping move: {}", currentObjectName);
            Blob blob = storage.get(BlobId.of(bucketName, currentObjectName));
            return FirestoreImage.builder()
                    .storageUrl(String.format("gs://%s/%s", bucketName, currentObjectName))
                    .bucketName(bucketName)
                    .objectName(currentObjectName)
                    .contentType(blob != null ? blob.getContentType() : null)
                    .sizeBytes(blob != null ? blob.getSize() : null)
                    .md5Hash(blob != null ? blob.getMd5() : null)
                    .uploadedAt(Instant.now())
                    .build();
        }

        // Extract filename from current path
        String filename = currentObjectName.substring(currentObjectName.lastIndexOf("/") + 1);
        String newObjectName = String.format("%s/%s/%s", PERMANENT_FOLDER, bottleCapId, filename);

        // Copy to new location
        BlobId sourceBlobId = BlobId.of(bucketName, currentObjectName);
        BlobId targetBlobId = BlobId.of(bucketName, newObjectName);

        Blob sourceBlob = storage.get(sourceBlobId);
        if (sourceBlob == null) {
            throw new RuntimeException("Source image not found: " + currentObjectName);
        }

        Storage.CopyRequest copyRequest = Storage.CopyRequest.newBuilder()
                .setSource(sourceBlobId)
                .setTarget(targetBlobId)
                .build();
        Blob copiedBlob = storage.copy(copyRequest).getResult();

        // Delete the old file
        storage.delete(sourceBlobId);

        log.info("Successfully moved image to: gs://{}/{}", bucketName, newObjectName);

        return FirestoreImage.builder()
                .storageUrl(String.format("gs://%s/%s", bucketName, newObjectName))
                .bucketName(bucketName)
                .objectName(newObjectName)
                .contentType(copiedBlob.getContentType())
                .sizeBytes(copiedBlob.getSize())
                .md5Hash(copiedBlob.getMd5())
                .uploadedAt(Instant.now())
                .build();
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
