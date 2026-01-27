package com.km.bottlecapcollector.cloud.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.km.bottlecapcollector.cloud.storage.api.StorageImage;
import com.km.bottlecapcollector.property.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudStorageServiceTests {

    private static final String BUCKET_NAME = "test-bucket";
    private static final String USER_ID = "user-123";
    private static final String COLLECTION_KEY = "collection-456";

    @Mock
    private Storage storage;

    @Mock
    private AppProperties appProperties;

    @Mock
    private MultipartFile multipartFile;

    @Mock
    private Blob blob;

    private CloudStorageService cloudStorageService;

    @BeforeEach
    void setUp() {
        when(appProperties.getCloudStorageBucketName()).thenReturn(BUCKET_NAME);
        cloudStorageService = new CloudStorageService(storage, appProperties);
    }

    @Test
    void uploadImage_shouldUploadFileAndReturnStorageImage() throws IOException {
        // Given
        String originalFilename = "test-image.jpg";
        String contentType = "image/jpeg";
        byte[] fileContent = "test content".getBytes();
        long fileSize = fileContent.length;
        String md5Hash = "abc123md5";

        when(multipartFile.getOriginalFilename()).thenReturn(originalFilename);
        when(multipartFile.getContentType()).thenReturn(contentType);
        when(multipartFile.getBytes()).thenReturn(fileContent);
        when(multipartFile.getSize()).thenReturn(fileSize);
        when(blob.getMd5()).thenReturn(md5Hash);
        when(storage.create(any(BlobInfo.class), eq(fileContent))).thenReturn(blob);

        // When
        StorageImage result = cloudStorageService.uploadImage(multipartFile, USER_ID, COLLECTION_KEY);

        // Then
        assertNotNull(result);
        assertEquals(BUCKET_NAME, result.getBucketName());
        assertTrue(result.getObjectName().startsWith(USER_ID + "/" + COLLECTION_KEY + "/"));
        assertTrue(result.getObjectName().endsWith(".jpg"));
        assertEquals(contentType, result.getContentType());
        assertEquals(Long.valueOf(fileSize), result.getSizeBytes());
        assertEquals(md5Hash, result.getMd5Hash());
        assertTrue(result.getStorageUrl().startsWith("gs://" + BUCKET_NAME + "/"));
        assertNotNull(result.getUploadedAt());

        // Verify storage.create was called with correct parameters
        ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
        verify(storage).create(blobInfoCaptor.capture(), eq(fileContent));

        BlobInfo capturedBlobInfo = blobInfoCaptor.getValue();
        assertEquals(BUCKET_NAME, capturedBlobInfo.getBucket());
        assertEquals(contentType, capturedBlobInfo.getContentType());
    }

    @Test
    void uploadImage_shouldRejectFileWithNullContentType() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn(null);

        // When / Then - security validation should reject files without valid image content type
        CloudStorageException exception = assertThrows(CloudStorageException.class, () ->
            cloudStorageService.uploadImage(multipartFile, USER_ID, COLLECTION_KEY)
        );
        assertTrue(exception.getMessage().contains("Invalid file type"));
    }

    @Test
    void uploadImage_shouldRejectNonImageContentType() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/octet-stream");

        // When / Then - security validation should reject non-image files
        CloudStorageException exception = assertThrows(CloudStorageException.class, () ->
            cloudStorageService.uploadImage(multipartFile, USER_ID, COLLECTION_KEY)
        );
        assertTrue(exception.getMessage().contains("Invalid file type"));
    }

    @Test
    void uploadImage_shouldHandleFileWithoutExtension() throws IOException {
        // Given
        String originalFilename = "testfile";
        byte[] fileContent = "test content".getBytes();

        when(multipartFile.getOriginalFilename()).thenReturn(originalFilename);
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getBytes()).thenReturn(fileContent);
        when(multipartFile.getSize()).thenReturn((long) fileContent.length);
        when(blob.getMd5()).thenReturn("hash");
        when(storage.create(any(BlobInfo.class), eq(fileContent))).thenReturn(blob);

        // When
        StorageImage result = cloudStorageService.uploadImage(multipartFile, USER_ID, COLLECTION_KEY);

        // Then
        assertNotNull(result);
        assertTrue(result.getObjectName().startsWith(USER_ID + "/" + COLLECTION_KEY + "/"));
        assertFalse(result.getObjectName().endsWith("."));
    }

    @Test
    void uploadImage_shouldHandleNullFilename() throws IOException {
        // Given
        byte[] fileContent = "test content".getBytes();

        when(multipartFile.getOriginalFilename()).thenReturn(null);
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getBytes()).thenReturn(fileContent);
        when(multipartFile.getSize()).thenReturn((long) fileContent.length);
        when(blob.getMd5()).thenReturn("hash");
        when(storage.create(any(BlobInfo.class), eq(fileContent))).thenReturn(blob);

        // When
        StorageImage result = cloudStorageService.uploadImage(multipartFile, USER_ID, COLLECTION_KEY);

        // Then
        assertNotNull(result);
        assertTrue(result.getObjectName().startsWith(USER_ID + "/" + COLLECTION_KEY + "/"));
    }

    @Test
    void downloadImage_shouldReturnImageBytes() {
        // Given
        String objectName = "item-123/image.jpg";
        byte[] expectedContent = "image bytes".getBytes();

        when(storage.get(any(BlobId.class))).thenReturn(blob);
        when(blob.getContent()).thenReturn(expectedContent);

        // When
        byte[] result = cloudStorageService.downloadImage(objectName);

        // Then
        assertArrayEquals(expectedContent, result);

        ArgumentCaptor<BlobId> blobIdCaptor = ArgumentCaptor.forClass(BlobId.class);
        verify(storage).get(blobIdCaptor.capture());
        assertEquals(BUCKET_NAME, blobIdCaptor.getValue().getBucket());
        assertEquals(objectName, blobIdCaptor.getValue().getName());
    }

    @Test
    void downloadImage_shouldThrowExceptionWhenImageNotFound() {
        // Given
        String objectName = "non-existent/image.jpg";
        when(storage.get(any(BlobId.class))).thenReturn(null);

        // When / Then
        assertThrows(CloudStorageException.class, () -> {
            cloudStorageService.downloadImage(objectName);
        });
    }

    @Test
    void deleteImage_shouldReturnTrueWhenDeleted() {
        // Given
        String objectName = "item-123/image.jpg";
        when(storage.delete(any(BlobId.class))).thenReturn(true);

        // When
        boolean result = cloudStorageService.deleteImage(objectName);

        // Then
        assertTrue(result);

        ArgumentCaptor<BlobId> blobIdCaptor = ArgumentCaptor.forClass(BlobId.class);
        verify(storage).delete(blobIdCaptor.capture());
        assertEquals(BUCKET_NAME, blobIdCaptor.getValue().getBucket());
        assertEquals(objectName, blobIdCaptor.getValue().getName());
    }

    @Test
    void deleteImage_shouldReturnFalseWhenNotFound() {
        // Given
        String objectName = "non-existent/image.jpg";
        when(storage.delete(any(BlobId.class))).thenReturn(false);

        // When
        boolean result = cloudStorageService.deleteImage(objectName);

        // Then
        assertFalse(result);
    }

    @Test
    void generateSignedUrl_shouldReturnSignedUrl() throws Exception {
        // Given
        String objectName = "item-123/image.jpg";
        int durationMinutes = 15;
        String expectedUrl = "https://storage.googleapis.com/signed-url";

        when(storage.signUrl(any(BlobInfo.class), eq((long) durationMinutes), eq(TimeUnit.MINUTES)))
                .thenReturn(new URL(expectedUrl));

        // When
        String result = cloudStorageService.generateSignedUrl(objectName);

        // Then
        assertEquals(expectedUrl, result);

        ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
        verify(storage).signUrl(blobInfoCaptor.capture(), eq((long) durationMinutes), eq(TimeUnit.MINUTES));

        BlobInfo capturedBlobInfo = blobInfoCaptor.getValue();
        assertEquals(BUCKET_NAME, capturedBlobInfo.getBucket());
        assertEquals(objectName, capturedBlobInfo.getName());
    }

    @Test
    void uploadImage_shouldGenerateUniqueObjectNames() throws IOException {
        // Given
        String originalFilename = "test.jpg";
        byte[] fileContent = "content".getBytes();

        when(multipartFile.getOriginalFilename()).thenReturn(originalFilename);
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getBytes()).thenReturn(fileContent);
        when(multipartFile.getSize()).thenReturn((long) fileContent.length);
        when(blob.getMd5()).thenReturn("hash");
        when(storage.create(any(BlobInfo.class), eq(fileContent))).thenReturn(blob);

        // When
        StorageImage result1 = cloudStorageService.uploadImage(multipartFile, USER_ID, COLLECTION_KEY);
        StorageImage result2 = cloudStorageService.uploadImage(multipartFile, USER_ID, COLLECTION_KEY);

        // Then
        assertNotEquals(result1.getObjectName(), result2.getObjectName());
    }
}
