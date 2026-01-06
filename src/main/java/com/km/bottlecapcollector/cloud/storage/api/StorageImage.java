package com.km.bottlecapcollector.cloud.storage.api;

import com.km.bottlecapcollector.color.HSBColor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Firestore embedded document representing image data stored in GCP Cloud Storage.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageImage {

    private String storageUrl;

    private String bucketName;

    private String objectName;

    private String contentType;

    private Long sizeBytes;

    private String md5Hash;

    private Integer width;

    private Integer height;

    private Instant uploadedAt;

    private HSBColor hsbColor;
}
