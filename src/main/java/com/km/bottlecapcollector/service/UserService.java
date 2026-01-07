package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.cloud.database.user.entity.UserCollectionEntity;
import com.km.bottlecapcollector.cloud.database.user.entity.UserEntity;
import com.km.bottlecapcollector.cloud.database.user.service.UserEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service responsible for user-related business logic.
 * Acts as a proxy layer on top of UserEntityService and provides
 * authentication utilities.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserEntityService userEntityService;

    /**
     * Extracts the user ID from the OAuth2 principal.
     *
     * @param principal the OAuth2 authenticated principal
     * @return the user ID
     * @throws IllegalStateException if user is not authenticated
     */
    public String getUserId(OAuth2AuthenticatedPrincipal principal) {
        return Optional.ofNullable(principal)
                .map(p -> p.getAttribute("sub"))
                .map(String.class::cast)
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));
    }

    /**
     * Gets all collections for a given user.
     *
     * @param userId the user ID
     * @return list of user collections
     */
    public List<UserCollectionEntity> getCollections(String userId) {
        return userEntityService.getCollections(userId);
    }

    /**
     * Gets the human-readable name for a collection key.
     *
     * @param userId the user ID
     * @param collectionKey the collection UUID key
     * @return the collection name or null if not found
     */
    public String getCollectionName(String userId, String collectionKey) {
        return userEntityService.getCollectionName(userId, collectionKey);
    }

    /**
     * Adds a collection to the user's set of collections.
     *
     * @param userId the user ID
     * @param collectionKey the UUID key for the collection
     * @param collectionName the human-readable name
     */
    public void addCollectionToUser(String userId, String collectionKey, String collectionName) {
        userEntityService.addCollectionToUser(userId, collectionKey, collectionName);
    }

    /**
     * Gets a user by ID.
     *
     * @param userId the user ID
     * @return the user entity
     */
    public UserEntity getUser(String userId) {
        return userEntityService.getUser(userId);
    }

    /**
     * Checks if a user exists.
     *
     * @param userId the user ID
     * @return true if user exists
     */
    public boolean userExists(String userId) {
        return userEntityService.userExists(userId);
    }
}
