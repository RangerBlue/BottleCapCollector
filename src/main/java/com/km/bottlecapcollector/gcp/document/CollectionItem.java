package com.km.bottlecapcollector.gcp.document;

import com.google.cloud.firestore.annotation.DocumentId;
import com.km.bottlecapcollector.gcp.storage.api.StorageImage;
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
public class CollectionItem {

    @DocumentId
    private String id;

    private String name;

    private String description;

    private Instant createdAt;

    private Instant updatedAt;

    private StorageImage image;

    private FirestoreVisionMetadata visionMetadata;

    private FirestoreEmbedding embedding;

    /**
     * Simple string tags (auto-generated from Vision API).
     */
    private List<String> tags;

    /**
     * Custom key-value tags provided by the user (e.g., "rank": "the best", "color": "blue").
     */
    private Map<String, String> customTags;

    private String userId;

    private String collectionName;
}
