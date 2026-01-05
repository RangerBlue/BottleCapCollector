package com.km.bottlecapcollector.firestore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for transferring FirestoreImage data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirestoreImageDto {

    private String objectName;

    private String contentType;

    private Long sizeBytes;

    private String md5Hash;

    private Integer width;

    private Integer height;

    private Instant uploadedAt;

    private FirestoreHSBColorDto hsbColor;

    /**
     * Short-lived signed URL for accessing the image.
     * This URL expires after a configured duration.
     */
    private String signedUrl;
}
