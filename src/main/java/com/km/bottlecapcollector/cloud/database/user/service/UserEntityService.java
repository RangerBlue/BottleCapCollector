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
