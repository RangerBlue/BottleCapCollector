package com.km.bottlecapcollector.cloud.database.document;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Firestore document representing a bottle cap with its associated image,
 * Vision API metadata, and Vertex AI embeddings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemEntity {

    @DocumentId
    private String id;

    private String name;

    private String description;

    private Instant createdAt;

    private Instant updatedAt;

    private StorageImageEntity image;

    private ImageAnalysisMetadataEntity visionMetadata;

    private EmbeddingEntity embedding;

    /**
     * Simple string tags (auto-generated from Vision API).
     */
    private List<String> tags;

    /**
     * Custom key-value tags provided by the user (e.g., "rank": "the best", "color": "blue").
     */
    private Map<String, String> customTags;

    private String userId;

    /**
     * UUID key used as Firestore collection name (safe, no special chars).
     */
    private String collectionKey;

    /**
     * Searchable tokens for efficient prefix search in Firestore.
     * Contains lowercase prefixes generated from: name, customTags values,
     * Vision API labels, text words, and logo descriptions.
     */
    private List<String> searchTokens;
}
