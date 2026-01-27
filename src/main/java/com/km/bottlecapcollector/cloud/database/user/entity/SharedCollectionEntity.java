package com.km.bottlecapcollector.cloud.database.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Embedded document representing a collection that has been shared WITH this user.
 * This is denormalized for efficient reads - when displaying "shared with me" lists,
 * we don't need to query multiple documents.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedCollectionEntity {

    /**
     * The collection key (used for accessing items).
     */
    private String collectionKey;

    /**
     * The human-readable name of the collection.
     */
    private String collectionName;

    /**
     * The user ID of the owner who shared the collection.
     */
    private String ownerUserId;

    /**
     * The name of the owner (for display purposes).
     */
    private String ownerName;

    /**
     * When the share was granted.
     */
    private Instant sharedAt;
}
