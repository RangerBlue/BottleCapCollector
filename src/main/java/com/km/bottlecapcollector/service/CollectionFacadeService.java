package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.api.model.request.CreateCollectionItemRequest;
import com.km.bottlecapcollector.api.model.request.UpdateCollectionItem;
import com.km.bottlecapcollector.api.model.response.CollectionItemResponse;
import com.km.bottlecapcollector.api.model.response.CollectionItemSummary;
import com.km.bottlecapcollector.api.model.response.UserCollectionResponse;
import com.km.bottlecapcollector.api.model.response.ValidateItemResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Facade service that handles OAuth2 principal translation and delegates
 * to CollectionService. This layer is responsible for extracting the user ID
 * from the authentication principal before calling collection operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CollectionFacadeService {

    private final UserService userService;
    private final CollectionService collectionService;

    /**
     * Gets all collections for the authenticated user.
     */
    public List<UserCollectionResponse> getUserCollections(OAuth2AuthenticatedPrincipal principal) {
        String userId = userService.getUserId(principal);
        log.info("Getting collections for user: {}", userId);
        return userService.getCollections(userId).stream()
                .map(c -> UserCollectionResponse.builder()
                        .collectionKey(c.getCollectionKey())
                        .collectionName(c.getCollectionName())
                        .build())
                .toList();
    }

    /**
     * Creates a new collection item for the authenticated user.
     */
    public CollectionItemResponse createItem(OAuth2AuthenticatedPrincipal principal,
                                             String collectionKey,
                                             CreateCollectionItemRequest request,
                                             MultipartFile file) {
        String userId = userService.getUserId(principal);
        log.info("User {} creating item in collection {}", userId, collectionKey);
        return collectionService.createCollectionItem(collectionKey, userId, request, file);
    }

    /**
     * Gets a specific collection item for the authenticated user.
     */
    public CollectionItemResponse getItem(OAuth2AuthenticatedPrincipal principal,
                                          String collectionKey,
                                          String id) {
        String userId = userService.getUserId(principal);
        log.trace("User {} getting item {} from collection {}", userId, id, collectionKey);
        return collectionService.getCollectionItem(collectionKey, id, userId);
    }

    /**
     * Searches items in a collection for the authenticated user.
     */
    public Page<@NotNull CollectionItemSummary> getItems(OAuth2AuthenticatedPrincipal principal,
                                                         String collectionKey,
                                                         String query,
                                                         Pageable pageable) {
        String userId = userService.getUserId(principal);
        log.info("User {} getting items from collection {} with query '{}', page: {}, size: {}",
                userId, collectionKey, query, pageable.getPageNumber(), pageable.getPageSize());
        return collectionService.searchItemsPaginated(collectionKey, query, userId, pageable);
    }

    /**
     * Updates a collection item for the authenticated user.
     */
    public CollectionItemResponse updateItem(OAuth2AuthenticatedPrincipal principal,
                                             String collectionKey,
                                             String id,
                                             UpdateCollectionItem request) {
        String userId = userService.getUserId(principal);
        log.info("User {} updating item {} in collection {}", userId, id, collectionKey);
        return collectionService.updateItem(collectionKey, id, userId, request);
    }

    /**
     * Updates the image of a collection item for the authenticated user.
     */
    public CollectionItemResponse updateImage(OAuth2AuthenticatedPrincipal principal,
                                              String collectionKey,
                                              String id,
                                              MultipartFile file) {
        String userId = userService.getUserId(principal);
        log.info("User {} updating image for item {} in collection {}", userId, id, collectionKey);
        return collectionService.updateImage(collectionKey, id, userId, file);
    }

    /**
     * Validates an item (checks for similar items) for the authenticated user.
     */
    public ValidateItemResponse validateItem(OAuth2AuthenticatedPrincipal principal,
                                             String collectionKey,
                                             MultipartFile file) {
        String userId = userService.getUserId(principal);
        log.info("User {} validating item in collection {}", userId, collectionKey);
        return collectionService.validateItem(collectionKey, userId, file);
    }

    /**
     * Deletes a collection item for the authenticated user.
     */
    public void deleteItem(OAuth2AuthenticatedPrincipal principal,
                           String collectionKey,
                           String id) {
        String userId = userService.getUserId(principal);
        log.info("User {} deleting item {} from collection {}", userId, id, collectionKey);
        collectionService.deleteItem(collectionKey, id, userId);
    }
}
