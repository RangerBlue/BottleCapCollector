package com.km.bottlecapcollector.cloud.database.document;

import com.google.cloud.firestore.annotation.DocumentId;
import com.km.bottlecapcollector.security.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
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

    private Instant createdAt;

    private Instant updatedAt;

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
