package com.km.bottlecapcollector.cloud.database.user.service;

import com.km.bottlecapcollector.cloud.database.user.entity.UserCollectionEntity;
import com.km.bottlecapcollector.cloud.database.user.entity.UserEntity;
import com.km.bottlecapcollector.cloud.database.exception.FirestoreDocumentNotFoundException;
import com.km.bottlecapcollector.cloud.database.user.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service layer for managing users in Firestore.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserEntityService {

    private final UserEntityRepository userRepository;

    /**
     * Gets all collections for a given user.
     *
     * @param userId the user ID
     * @return list of user collections (key + name)
     * @throws FirestoreDocumentNotFoundException if user not found
     */
    public List<UserCollectionEntity> getCollections(String userId) {
        log.trace("Getting collections for user: {}", userId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new FirestoreDocumentNotFoundException("users", userId));

        List<UserCollectionEntity> collections = user.getCollections();
        return collections != null ? collections : new ArrayList<>();
    }

    /**
     * Gets the human-readable name for a collection key.
     *
     * @param userId the user ID
     * @param collectionKey the collection UUID key
     * @return the collection name or null if not found
     */
    public String getCollectionName(String userId, String collectionKey) {
        log.trace("Getting collection name for key: {} user: {}", collectionKey, userId);
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        return user.getCollectionName(collectionKey);
    }

    /**
     * Adds a collection to the user's set of collections.
     * Creates the user if they don't exist.
     *
     * @param userId the user ID
     * @param collectionKey the UUID key for the collection
     * @param collectionName the human-readable name
     */
    public void addCollectionToUser(String userId, String collectionKey, String collectionName) {
        log.info("Adding collection key='{}' name='{}' to user: {}", collectionKey, collectionName, userId);

        UserEntity user = userRepository.findById(userId)
                .orElse(UserEntity.builder()
                        .id(userId)
                        .createdAt(Instant.now())
                        .build());

        if (user.addCollection(collectionKey, collectionName)) {
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);
            log.info("Added collection key='{}' to user: {}", collectionKey, userId);
        } else {
            log.trace("Collection key='{}' already exists for user: {}", collectionKey, userId);
        }
    }

    /**
     * Merges new custom tag keys into a collection's available tags.
     * This allows the frontend to suggest existing tags for consistency.
     *
     * @param userId the user ID
     * @param collectionKey the collection UUID key
     * @param tagKeys the custom tag keys to add
     */
    public void mergeCollectionAvailableTags(String userId, String collectionKey, Set<String> tagKeys) {
        if (tagKeys == null || tagKeys.isEmpty()) {
            return;
        }
        log.trace("Merging {} tag keys to collection '{}' for user: {}", tagKeys.size(), collectionKey, userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new FirestoreDocumentNotFoundException("users", userId));

        UserCollectionEntity collection = user.getCollections().stream()
                .filter(c -> c.getCollectionKey().equals(collectionKey))
                .findFirst()
                .orElse(null);

        if (collection == null) {
            log.warn("Collection '{}' not found for user: {}", collectionKey, userId);
            return;
        }

        List<String> existingTags = collection.getAvailableTags();
        if (existingTags == null) {
            existingTags = new ArrayList<>();
            collection.setAvailableTags(existingTags);
        }

        int addedCount = 0;
        for (String tag : tagKeys) {
            if (!existingTags.contains(tag)) {
                existingTags.add(tag);
                addedCount++;
            }
        }

        if (addedCount > 0) {
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);
            log.info("Added {} new tag keys to collection '{}' for user: {}",
                    addedCount, collectionKey, userId);
        }
    }

    /**
     * Gets available custom tag keys for a collection.
     *
     * @param userId the user ID
     * @param collectionKey the collection UUID key
     * @return list of available tag keys
     */
    public List<String> getCollectionAvailableTags(String userId, String collectionKey) {
        log.trace("Getting available tags for collection '{}' user: {}", collectionKey, userId);

        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getCollections() == null) {
            return new ArrayList<>();
        }

        return user.getCollections().stream()
                .filter(c -> c.getCollectionKey().equals(collectionKey))
                .findFirst()
                .map(UserCollectionEntity::getAvailableTags)
                .orElse(new ArrayList<>());
    }

    /**
     * Gets a user by ID.
     *
     * @param userId the user ID
     * @return the user
     * @throws FirestoreDocumentNotFoundException if user not found
     */
    public UserEntity getUser(String userId) {
        log.trace("Getting user: {}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new FirestoreDocumentNotFoundException("users", userId));
    }

    /**
     * Checks if a user exists.
     *
     * @param userId the user ID
     * @return true if user exists
     */
    public boolean userExists(String userId) {
        return userRepository.existsById(userId);
    }
}
