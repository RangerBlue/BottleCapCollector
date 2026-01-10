package com.km.bottlecapcollector.cloud.database.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Embedded document representing a user's collection.
 * The collectionKey is a UUID used for database operations (safe for Firestore collection names).
 * The collectionName is a human-readable name for display purposes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCollectionEntity {

    /**
     * UUID key used as Firestore collection name (safe, no special chars).
     */
    private String collectionKey;

    /**
     * Human-readable name for display purposes.
     */
    private String collectionName;

    /**
     * Available custom tag keys used across all items in this collection.
     * This allows the frontend to suggest existing tags for consistency.
     */
    @Builder.Default
    private List<String> availableTags = new ArrayList<>();
}
