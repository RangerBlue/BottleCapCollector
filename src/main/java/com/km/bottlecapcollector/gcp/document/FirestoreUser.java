package com.km.bottlecapcollector.gcp.document;

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
public class FirestoreUser {

    @DocumentId
    private String id;

    private String email;

    private String name;

    private Role role;

    @Builder.Default
    private List<String> collectionNames = new ArrayList<>();

    private Instant createdAt;

    private Instant updatedAt;

    /**
     * Adds a collection name to the user's list of collections if not already present.
     * @param collectionName the collection name to add
     * @return true if the collection was added (not already present)
     */
    public boolean addCollectionName(String collectionName) {
        if (collectionNames == null) {
            collectionNames = new ArrayList<>();
        }
        if (!collectionNames.contains(collectionName)) {
            return collectionNames.add(collectionName);
        }
        return false;
    }
}
