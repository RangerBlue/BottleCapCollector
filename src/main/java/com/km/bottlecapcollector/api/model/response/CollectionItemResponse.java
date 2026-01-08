package com.km.bottlecapcollector.api.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO for transferring FirestoreBottleCap data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionItemResponse {

    private String id;

    private String name;

    private String description;

    private Instant createdAt;

    private Instant updatedAt;

    private ImageResponse image;

    private ImageAnalysisMetadataResponse visionMetadata;

    private List<String> tags;

    private Map<String, String> customTags;

    private String userId;

    private String collectionKey;

    private String collectionName;
}
