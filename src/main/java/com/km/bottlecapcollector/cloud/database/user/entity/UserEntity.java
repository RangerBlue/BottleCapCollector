package com.km.bottlecapcollector.cloud.database.user.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.km.bottlecapcollector.security.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Firestore document representing a user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @DocumentId
    private String id;

    private String email;

    private String name;

    private Role role;

    @Builder.Default
    private List<UserCollectionEntity> collections = new ArrayList<>();

    /**
     * Shares this user has granted to others (owner side of sharing).
     */
    @Builder.Default
    private List<CollectionShareEntity> sharesGranted = new ArrayList<>();

    /**
     * Collections that have been shared with this user (recipient side of sharing).
     */
    @Builder.Default
    private List<SharedCollectionEntity> sharedWithMe = new ArrayList<>();

    private Instant createdAt;

    private Instant updatedAt;

    /**
     * Maximum number of items this user can have across all collections.
     * If null, the global default from AppProperties is used.
     */
    private Integer maxItems;

    /**
     * Current count of identification API calls for the current day.
     */
    private Integer identificationUsageCount;

    /**
     * Date when the identification usage counter was last reset.
     */
    private Instant lastIdentificationResetDate;

    /**
     * Per-user override for max identifications per day.
     * If null, the global default from AppProperties is used.
     */
    private Integer maxIdentificationsPerDay;

    /**
     * Adds a collection to the user's list if not already present (by key).
     * @param collectionKey the UUID key for the collection
     * @param collectionName the human-readable name
     * @return true if the collection was added (not already present)
     */
    public boolean addCollection(String collectionKey, String collectionName) {
        if (collections == null) {
            collections = new ArrayList<>();
        }
        boolean exists = collections.stream()
                .anyMatch(c -> c.getCollectionKey().equals(collectionKey));
        if (!exists) {
            return collections.add(UserCollectionEntity.builder()
                    .collectionKey(collectionKey)
                    .collectionName(collectionName)
                    .build());
        }
        return false;
    }

    /**
     * Finds the human-readable name for a collection key.
     * @param collectionKey the UUID key
     * @return the collection name or null if not found
     */
    public String getCollectionName(String collectionKey) {
        if (collections == null) {
            return null;
        }
        return collections.stream()
                .filter(c -> c.getCollectionKey().equals(collectionKey))
                .map(UserCollectionEntity::getCollectionName)
                .findFirst()
                .orElse(null);
    }
}
