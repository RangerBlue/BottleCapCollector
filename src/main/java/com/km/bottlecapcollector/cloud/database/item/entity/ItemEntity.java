package com.km.bottlecapcollector.cloud.database.item.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    private List<String> tags;

    private Map<String, String> customTags;

    private String userId;

    private String collectionKey;

    private List<String> searchTokens;
}
