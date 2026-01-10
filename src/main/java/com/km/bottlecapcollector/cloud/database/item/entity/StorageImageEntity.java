package com.km.bottlecapcollector.cloud.database.item.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageImageEntity {
    private String storageUrl;
    private String bucketName;
    private String objectName;
    private String contentType;
    private Long sizeBytes;
    private String md5Hash;
    private Integer width;
    private Integer height;
    private Instant uploadedAt;
    private HSBColorEntity hsbColor;
    private String hsbBucket;
}
