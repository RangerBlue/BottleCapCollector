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
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserEntityService userEntityService;

    public String getUserId(OAuth2AuthenticatedPrincipal principal) {
        return Optional.ofNullable(principal)
                .map(p -> p.getAttribute("sub"))
                .map(String.class::cast)
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));
    }

    public List<UserCollectionEntity> getCollections(String userId) {
        return userEntityService.getCollections(userId);
    }

    public String getCollectionName(String userId, String collectionKey) {
        return userEntityService.getCollectionName(userId, collectionKey);
    }

    public void addCollectionToUser(String userId, String collectionKey, String collectionName) {
        userEntityService.addCollectionToUser(userId, collectionKey, collectionName);
    }

    public UserEntity getUser(String userId) {
        return userEntityService.getUser(userId);
    }

    public boolean userExists(String userId) {
        return userEntityService.userExists(userId);
    }

    public void mergeCollectionAvailableTags(String userId, String collectionKey, Set<String> tagKeys) {
        userEntityService.mergeCollectionAvailableTags(userId, collectionKey, tagKeys);
    }

    public List<String> getCollectionAvailableTags(String userId, String collectionKey) {
        return userEntityService.getCollectionAvailableTags(userId, collectionKey);
    }

    public boolean ownsCollection(String userId, String collectionKey) {
        try {
            List<UserCollectionEntity> collections = getCollections(userId);
            return collections.stream()
                    .anyMatch(c -> c.getCollectionKey().equals(collectionKey));
        } catch (Exception e) {
            log.warn("Failed to check ownership for user {} collection {}: {}", userId, collectionKey, e.getMessage());
            return false;
        }
    }
}
