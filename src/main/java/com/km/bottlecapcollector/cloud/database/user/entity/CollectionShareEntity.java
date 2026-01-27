package com.km.bottlecapcollector.cloud.database.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Embedded document representing a share that the user has granted.
 * Tracks who the owner has shared a specific collection with.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionShareEntity {

    /**
     * The collection key that was shared.
     */
    private String collectionKey;

    /**
     * The user ID of the person the collection was shared with.
     */
    private String sharedWithUserId;

    /**
     * The email of the person the collection was shared with (for display purposes).
     */
    private String sharedWithEmail;

    /**
     * When the share was created.
     */
    private Instant sharedAt;
}
