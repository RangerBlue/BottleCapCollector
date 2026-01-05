package com.km.bottlecapcollector.firestore.service;

import com.km.bottlecapcollector.firestore.document.FirestoreUser;
import com.km.bottlecapcollector.firestore.exception.FirestoreDocumentNotFoundException;
import com.km.bottlecapcollector.firestore.repository.FirestoreUserRepository;
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
public class FirestoreUserService {

    private final FirestoreUserRepository userRepository;

    /**
     * Gets the collection names for a given user.
     *
     * @param userId the user ID
     * @return list of collection names the user has items in
     * @throws FirestoreDocumentNotFoundException if user not found
     */
    public List<String> getCollectionNames(String userId) {
        log.trace("Getting collection names for user: {}", userId);
        FirestoreUser user = userRepository.findById(userId)
                .orElseThrow(() -> new FirestoreDocumentNotFoundException("users", userId));

        List<String> collectionNames = user.getCollectionNames();
        return collectionNames != null ? collectionNames : new ArrayList<>();
    }

    /**
     * Adds a collection name to the user's set of collections.
     * Creates the user if they don't exist.
     *
     * @param userId the user ID
     * @param collectionName the collection name to add
     */
    public void addCollectionToUser(String userId, String collectionName) {
        log.info("Adding collection '{}' to user: {}", collectionName, userId);

        FirestoreUser user = userRepository.findById(userId)
                .orElse(FirestoreUser.builder()
                        .id(userId)
                        .createdAt(Instant.now())
                        .build());

        if (user.addCollectionName(collectionName)) {
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);
            log.info("Added collection '{}' to user: {}", collectionName, userId);
        } else {
            log.trace("Collection '{}' already exists for user: {}", collectionName, userId);
        }
    }

    /**
     * Gets a user by ID.
     *
     * @param userId the user ID
     * @return the user
     * @throws FirestoreDocumentNotFoundException if user not found
     */
    public FirestoreUser getUser(String userId) {
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
