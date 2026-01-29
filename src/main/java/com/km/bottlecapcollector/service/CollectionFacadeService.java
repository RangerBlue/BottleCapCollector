package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.api.handler.exception.AppBadRequestException;
import com.km.bottlecapcollector.api.handler.exception.AppForbiddenException;
import com.km.bottlecapcollector.api.model.request.CreateCollectionItemRequest;
import com.km.bottlecapcollector.property.AppProperties;
import com.km.bottlecapcollector.api.model.request.ShareCollectionRequest;
import com.km.bottlecapcollector.api.model.request.UpdateCollectionItem;
import com.km.bottlecapcollector.api.model.response.CollectionItemResponse;
import com.km.bottlecapcollector.api.model.response.CollectionItemSummary;
import com.km.bottlecapcollector.api.model.response.CollectionShareResponse;
import com.km.bottlecapcollector.api.model.response.ItemIdentificationResponse;
import com.km.bottlecapcollector.api.model.response.RateLimitInfo;
import com.km.bottlecapcollector.api.model.response.ShareCollectionResponse;
import com.km.bottlecapcollector.api.model.response.SharedCollectionResponse;
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
    private final RateLimitService rateLimitService;
    private final CollectionShareService collectionShareService;
    private final AppProperties appProperties;

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
     * Gets a specific collection item.
     * Works for both owned collections and collections shared with the user.
     */
    public CollectionItemResponse getItem(OAuth2AuthenticatedPrincipal principal,
                                          String collectionKey,
                                          String id) {
        String userId = userService.getUserId(principal);
        String ownerUserId = collectionShareService.resolveCollectionOwner(userId, collectionKey);
        log.trace("User {} getting item {} from collection {} (owner: {})", userId, id, collectionKey, ownerUserId);
        return collectionService.getCollectionItem(collectionKey, id, ownerUserId);
    }

    /**
     * Searches items in a collection.
     * Works for both owned collections and collections shared with the user.
     */
    public Page<@NotNull CollectionItemSummary> getItems(OAuth2AuthenticatedPrincipal principal,
                                                         String collectionKey,
                                                         String query,
                                                         Pageable pageable) {
        String userId = userService.getUserId(principal);
        String ownerUserId = collectionShareService.resolveCollectionOwner(userId, collectionKey);
        log.info("User {} getting items from collection {} (owner: {}) with query '{}', page: {}, size: {}",
                userId, collectionKey, ownerUserId, query, pageable.getPageNumber(), pageable.getPageSize());
        return collectionService.searchItemsPaginated(collectionKey, query, ownerUserId, pageable);
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

    /**
     * Gets available custom tag keys for a collection.
     */
    public List<String> getCollectionAvailableTags(OAuth2AuthenticatedPrincipal principal,
                                                    String collectionKey) {
        String userId = userService.getUserId(principal);
        log.trace("User {} getting available tags for collection {}", userId, collectionKey);
        return userService.getCollectionAvailableTags(userId, collectionKey);
    }

    /**
     * Identifies an item using Gemini Vision from an uploaded file.
     * Uses AI to determine what the item is.
     * This helps users name/describe items before creating them.
     * Rate-limited: USER role has daily limits, ADMIN has unlimited access.
     *
     * @param principal the authenticated user
     * @param file the image file
     * @return identification results with rate limit info
     */
    public ItemIdentificationResponse identifyItem(OAuth2AuthenticatedPrincipal principal,
                                                   MultipartFile file) {
        String userId = userService.getUserId(principal);
        log.info("User {} identifying item from uploaded file", userId);

        // Check and increment rate limit (throws RateLimitExceededException if exceeded)
        RateLimitInfo rateLimitInfo = rateLimitService.checkAndIncrementIdentificationUsage(userId);

        // Perform identification
        ItemIdentificationResponse response = collectionService.identifyItem(userId, file);

        // Attach rate limit info to response
        response.setRateLimit(rateLimitInfo);

        return response;
    }

    /**
     * Shares a collection with another user.
     */
    public ShareCollectionResponse shareCollection(OAuth2AuthenticatedPrincipal principal,
                                                   String collectionKey,
                                                   ShareCollectionRequest request) {
        String userId = userService.getUserId(principal);
        log.info("User {} sharing collection {} with {}", userId, collectionKey, request.getEmail());
        return collectionShareService.shareCollection(userId, collectionKey, request.getEmail());
    }

    /**
     * Revokes a share from a user.
     */
    public void revokeShare(OAuth2AuthenticatedPrincipal principal,
                            String collectionKey,
                            String targetUserId) {
        String userId = userService.getUserId(principal);
        log.info("User {} revoking share of collection {} from user {}", userId, collectionKey, targetUserId);
        collectionShareService.revokeShare(userId, collectionKey, targetUserId);
    }

    /**
     * Gets all shares for a collection.
     */
    public List<CollectionShareResponse> getCollectionShares(OAuth2AuthenticatedPrincipal principal,
                                                              String collectionKey) {
        String userId = userService.getUserId(principal);
        log.trace("User {} getting shares for collection {}", userId, collectionKey);
        return collectionShareService.getSharesForCollection(userId, collectionKey);
    }

    /**
     * Gets collections shared with the current user.
     */
    public List<SharedCollectionResponse> getSharedWithMeCollections(OAuth2AuthenticatedPrincipal principal) {
        String userId = userService.getUserId(principal);
        log.trace("User {} getting collections shared with them", userId);
        return collectionShareService.getCollectionsSharedWithUser(userId);
    }

    /**
     * Deletes a collection and all its items asynchronously.
     * Also revokes all shares for this collection.
     * Items are deleted in the background to handle large collections.
     *
     * @param principal the authenticated user
     * @param collectionKey the collection to delete
     */
    public void deleteCollection(OAuth2AuthenticatedPrincipal principal, String collectionKey) {
        String userId = userService.getUserId(principal);
        log.info("User {} initiating deletion of collection {}", userId, collectionKey);

        // Prevent deletion of legacy collection
        if (collectionKey.equals(appProperties.getLegacyCollectionKey())) {
            log.warn("User {} attempted to delete legacy collection {}", userId, collectionKey);
            throw new AppBadRequestException("Legacy collection cannot be deleted");
        }

        // Defense in depth: verify ownership before any action
        if (!userService.ownsCollection(userId, collectionKey)) {
            log.warn("User {} attempted to delete collection {} they don't own", userId, collectionKey);
            throw new AppForbiddenException("You do not own this collection");
        }

        // Remove collection from user and revoke all shares first
        // This prevents new items from being added and removes access immediately
        collectionShareService.removeCollectionAndAllShares(userId, collectionKey);

        // Delete all items in the collection asynchronously (including images)
        // Ownership already verified above, collection removed from user's list
        collectionService.deleteAllItemsInCollectionAsync(collectionKey, userId);

        log.info("User {} initiated async deletion of collection {}", userId, collectionKey);
    }
}
