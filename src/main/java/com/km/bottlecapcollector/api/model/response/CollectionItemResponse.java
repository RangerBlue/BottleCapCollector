package com.km.bottlecapcollector.api.model.response;

import com.km.bottlecapcollector.cloud.dto.FirestoreImageDto;
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

    private FirestoreImageDto image;

    private ImageAnalysisMetadataResponse visionMetadata;

    /**
     * Simple string tags (auto-generated from Vision API).
     */
    private List<String> tags;

    /**
     * Custom key-value tags provided by the user.
     */
    private Map<String, String> customTags;

    private String userId;


    private String collectionType;
}
