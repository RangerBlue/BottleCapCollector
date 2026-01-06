package com.km.bottlecapcollector.cloud.database.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
