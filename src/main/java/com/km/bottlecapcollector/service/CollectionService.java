package com.km.bottlecapcollector.service;

import com.google.cloud.firestore.Query;
import com.km.bottlecapcollector.api.handler.exception.AppBadRequestException;
import com.km.bottlecapcollector.api.handler.exception.AppForbiddenException;
import com.km.bottlecapcollector.cloud.database.user.entity.UserEntity;
import com.km.bottlecapcollector.api.model.request.CreateCollectionItemRequest;
import com.km.bottlecapcollector.api.model.request.UpdateCollectionItem;
import com.km.bottlecapcollector.api.model.response.CollectionItemResponse;
import com.km.bottlecapcollector.api.model.response.CollectionItemSummary;
import com.km.bottlecapcollector.api.model.response.ValidateItemResponse;
import com.km.bottlecapcollector.cloud.database.item.entity.ItemEntity;
import com.km.bottlecapcollector.cloud.database.item.service.ItemEntityService;
import com.km.bottlecapcollector.cloud.database.mapper.EntityDocumentMapper;
import com.km.bottlecapcollector.cloud.image.analysis.api.ImageAnalysisMetadata;
import com.km.bottlecapcollector.cloud.image.analysis.vision.VisionApiService;
import com.km.bottlecapcollector.cloud.image.ml.api.Embedding;
import com.km.bottlecapcollector.cloud.image.ml.api.EmbeddingService;
import com.km.bottlecapcollector.api.mapper.ApiMapper;
import com.km.bottlecapcollector.cloud.service.SearchTokenService;
import com.km.bottlecapcollector.cloud.storage.CloudStorageService;
import com.km.bottlecapcollector.cloud.storage.api.StorageImage;
import com.km.bottlecapcollector.color.HSBColor;
import com.km.bottlecapcollector.color.HSBColorService;
import com.km.bottlecapcollector.property.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for collection item business logic.
 * Orchestrates calls to external APIs (Cloud Storage, Vision API, Embedding Service)
 * and delegates database operations to ItemEntityService.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CollectionService {

    private final ItemEntityService itemEntityService;
    private final UserService userService;
    private final CloudStorageService cloudStorageService;
    private final VisionApiService visionApiService;
    private final EmbeddingService embeddingService;
    private final SearchTokenService searchTokenService;
    private final SimilarityService similarityService;
    private final AppProperties appProperties;
    private static final ApiMapper apiMapper = ApiMapper.INSTANCE;
    private static final EntityDocumentMapper documentMapper = EntityDocumentMapper.INSTANCE;

    public CollectionItemResponse createCollectionItem(String collectionKey, String userId,
                                                       CreateCollectionItemRequest request, MultipartFile file) {
        log.info("Creating new collection item: {} in collection: {} for user: {}", request.getName(), collectionKey,
                userId);

        checkItemLimit(userId);

        ItemEntity item = ItemEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .tags(request.getTags() != null ? new ArrayList<>(request.getTags()) : new ArrayList<>())
                .customTags(request.getCustomTags())
                .userId(userId)
                .collectionKey(collectionKey)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ItemEntity savedItem = itemEntityService.save(collectionKey, item);
        String itemId = savedItem.getId();
        log.info("Created collection item with id: {}", itemId);

        try {
            processAndAttachImage(savedItem, file);

            List<String> searchTokens = searchTokenService.generateTokens(savedItem);
            savedItem.setSearchTokens(searchTokens);
            log.info("Generated {} search tokens for item: {}", searchTokens.size(), itemId);

            savedItem.setUpdatedAt(Instant.now());
            ItemEntity finalItem = itemEntityService.save(collectionKey, savedItem);
            log.info("Successfully created and processed collection item with id: {}", itemId);

            userService.addCollectionToUser(userId, collectionKey, request.getCollectionName());
            mergeCustomTagsToCollection(userId, collectionKey, request.getCustomTags());

            CollectionItemResponse response = apiMapper.toResponse(finalItem);
            response.setCollectionName(request.getCollectionName());
            return enrichWithSignedUrl(response);

        } catch (Exception e) {
            log.error("Error during collection item creation, cleaning up: {}", itemId, e);
            try {
                if (savedItem.getImage() != null && savedItem.getImage().getObjectName() != null) {
                    cloudStorageService.deleteImage(savedItem.getImage().getObjectName());
                }
                itemEntityService.deleteById(collectionKey, itemId);
            } catch (Exception cleanupError) {
                log.error("Failed to clean up collection item: {}", itemId, cleanupError);
            }
            throw new AppBadRequestException("Failed to create collection item: " + e.getMessage(), e);
        }
    }

    public CollectionItemResponse getCollectionItem(String collectionKey, String id, String userId) {
        log.trace("Getting item with id: {} from collection: {} for user: {}", id, collectionKey, userId);
        ItemEntity item = itemEntityService.findByIdAndUserIdOrThrow(collectionKey, id, userId);
        CollectionItemResponse response = apiMapper.toResponse(item);
        response.setCollectionName(userService.getCollectionName(userId, collectionKey));
        return enrichWithSignedUrl(response);
    }

    public Page<@NotNull  CollectionItemSummary> searchItemsPaginated(String collectionKey, String query,
                                                                      String userId, Pageable pageable) {
        log.info("Searching items in collection: {} with query: '{}' for user: {}, page: {}, size: {}",
                collectionKey, query, userId, pageable.getPageNumber(), pageable.getPageSize());

        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();
        Query.Direction sortDirection = extractSortDirection(pageable);

        List<ItemEntity> items;
        long totalElements;

        if (query == null || query.isBlank()) {
            items = itemEntityService.findByUserId(collectionKey, userId, limit, offset, sortDirection);
            totalElements = itemEntityService.countByUserId(collectionKey, userId);
        } else {
            String searchToken = query.toLowerCase().trim();
            items = itemEntityService.findBySearchTokenAndUserId(collectionKey, searchToken, userId, limit, offset, sortDirection);
            totalElements = itemEntityService.countBySearchTokenAndUserId(collectionKey, searchToken, userId);
        }

        log.info("Returning page {} of {} items (total: {})", pageable.getPageNumber(), items.size(), totalElements);
        List<CollectionItemSummary> content = enrichSummariesWithSignedUrl(apiMapper.toSummaryList(items));
        return new PageImpl<>(content, pageable, totalElements);
    }

    private Query.Direction extractSortDirection(Pageable pageable) {
        Sort sort = pageable.getSort();
        Sort.Order createdAtOrder = sort.getOrderFor("createdAt");
        if (createdAtOrder != null && createdAtOrder.isAscending()) {
            return Query.Direction.ASCENDING;
        }
        return Query.Direction.DESCENDING;
    }

    public CollectionItemResponse updateItem(String collectionKey, String id, String userId, UpdateCollectionItem request) {
        log.info("Updating item with id: {} in collection: {} for user: {}", id, collectionKey, userId);

        ItemEntity item = itemEntityService.findByIdAndUserIdOrThrow(collectionKey, id, userId);
        boolean needsTokenRegeneration = false;

        if (request.getName() != null) {
            item.setName(request.getName());
            needsTokenRegeneration = true;
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getTags() != null) {
            item.setTags(request.getTags());
        }
        if (request.getCustomTags() != null) {
            item.setCustomTags(request.getCustomTags());
            needsTokenRegeneration = true;
            mergeCustomTagsToCollection(userId, collectionKey, request.getCustomTags());
        }

        if (needsTokenRegeneration) {
            List<String> searchTokens = searchTokenService.generateTokens(item);
            item.setSearchTokens(searchTokens);
            log.info("Regenerated {} search tokens for updating item: {}", searchTokens.size(), id);
        }

        item.setUpdatedAt(Instant.now());
        ItemEntity saved = itemEntityService.save(collectionKey, item);
        log.info("Successfully updated item with id: {}", id);

        CollectionItemResponse response = apiMapper.toResponse(saved);
        response.setCollectionName(userService.getCollectionName(userId, collectionKey));
        return enrichWithSignedUrl(response);
    }

    public CollectionItemResponse updateImage(String collectionKey, String id, String userId, MultipartFile file) {
        log.info("Updating image for item with id: {} in collection: {} for user: {}", id, collectionKey, userId);

        ItemEntity item = itemEntityService.findByIdAndUserIdOrThrow(collectionKey, id, userId);

        if (item.getImage() != null && item.getImage().getObjectName() != null) {
            log.info("Deleting old image: {}", item.getImage().getObjectName());
            cloudStorageService.deleteImage(item.getImage().getObjectName());
        }

        item.setTags(null);
        processAndAttachImage(item, file);

        List<String> searchTokens = searchTokenService.generateTokens(item);
        item.setSearchTokens(searchTokens);
        log.info("Regenerated {} search tokens for updating image item: {}", searchTokens.size(), id);

        item.setUpdatedAt(Instant.now());
        ItemEntity saved = itemEntityService.save(collectionKey, item);
        log.info("Successfully updated image for item with id: {}", id);

        CollectionItemResponse response = apiMapper.toResponse(saved);
        response.setCollectionName(userService.getCollectionName(userId, collectionKey));
        return enrichWithSignedUrl(response);
    }

    public void deleteItem(String collectionKey, String id, String userId) {
        log.info("Deleting item with id: {} from collection: {} for user: {}", id, collectionKey, userId);

        ItemEntity item = itemEntityService.findByIdAndUserIdOrThrow(collectionKey, id, userId);
        String objectName = item.getImage() != null ? item.getImage().getObjectName() : null;

        itemEntityService.deleteById(collectionKey, id);
        log.info("Deleted item from database: {}", id);

        if (objectName != null) {
            try {
                cloudStorageService.deleteImage(objectName);
                log.info("Deleted image from storage: {}", objectName);
            } catch (Exception e) {
                log.error("Failed to delete image from storage: {}, item already deleted from database", objectName, e);
            }
        }

        log.info("Successfully deleted item with id: {}", id);
    }

    public ValidateItemResponse validateItem(String collectionKey, String userId, MultipartFile file) {
        return similarityService.findSimilarItems(collectionKey, userId, file);
    }

    private void processAndAttachImage(ItemEntity item, MultipartFile file) {
        String userId = item.getUserId();
        String collectionKey = item.getCollectionKey();

        StorageImage image = cloudStorageService.uploadImage(file, userId, collectionKey);
        log.info("Uploaded image to Cloud Storage: {}", image.getObjectName());

        HSBColor hsbColor = HSBColorService.calculateColor(file);
        image.setHsbColor(hsbColor);
        image.setHsbBucket(HSBColorService.toBucket(hsbColor));
        item.setImage(documentMapper.toEntity(image));
        log.info("Calculated HSB color - H:{}, S:{}, B:{}, bucket: {}", hsbColor.getHue(), hsbColor.getSaturation(),
                hsbColor.getBrightness(), image.getHsbBucket());

        ImageAnalysisMetadata visionMetadata = visionApiService.analyzeImageFromFile(file);
        item.setVisionMetadata(documentMapper.toEntity(visionMetadata));
        log.info("Analyzed image with Vision API");

        if (embeddingService.isAvailable()) {
            Embedding embedding = embeddingService.generateEmbedding(file);
            if (embedding != null) {
                item.setEmbedding(documentMapper.toEntity(embedding));
                log.info("Generated embedding with {} dimensions", embedding.getDimensions());
            }
        } else {
            log.warn("Embedding service not available, skipping embedding generation");
        }

        List<String> extractedTags = visionApiService.extractTags(visionMetadata);
        List<String> existingTags = item.getTags();
        if (existingTags == null) {
            item.setTags(new ArrayList<>(extractedTags));
        } else {
            for (String tag : extractedTags) {
                if (!existingTags.contains(tag)) {
                    existingTags.add(tag);
                }
            }
        }
        log.info("Extracted {} tags from Vision API", extractedTags.size());
    }


    private CollectionItemResponse enrichWithSignedUrl(CollectionItemResponse response) {
        if (response != null && response.getImage() != null && response.getImage().getObjectName() != null) {
            String signedUrl = cloudStorageService.generateSignedUrl(
                    response.getImage().getObjectName());
            response.getImage().setSignedUrl(signedUrl);
        }
        return response;
    }

    private List<CollectionItemSummary> enrichSummariesWithSignedUrl(List<CollectionItemSummary> summaries) {
        summaries.forEach(summary -> {
            if (summary != null && summary.getObjectName() != null) {
                String signedUrl = cloudStorageService.generateSignedUrl(
                        summary.getObjectName());
                summary.setSignedUrl(signedUrl);
            }
        });
        return summaries;
    }

    private void mergeCustomTagsToCollection(String userId, String collectionKey, Map<String, String> customTags) {
        if (customTags != null && !customTags.isEmpty()) {
            userService.mergeCollectionAvailableTags(userId, collectionKey, new HashSet<>(customTags.keySet()));
        }
    }

    private void checkItemLimit(String userId) {
        long currentCount = itemEntityService.countAllByUserId(userId);
        int maxItems = getMaxItemsForUser(userId);

        if (currentCount >= maxItems) {
            log.warn("User {} has reached item limit: {}/{}", userId, currentCount, maxItems);
            throw new AppForbiddenException(
                    String.format("Item limit exceeded. You have %d items, maximum allowed is %d.", currentCount, maxItems));
        }
        log.trace("User {} item count: {}/{}", userId, currentCount, maxItems);
    }

    private int getMaxItemsForUser(String userId) {
        try {
            UserEntity user = userService.getUser(userId);
            if (user.getMaxItems() != null) {
                return user.getMaxItems();
            }
        } catch (Exception e) {
            log.trace("User {} not found, using global default", userId);
        }
        return appProperties.getMaxItemsPerUser();
    }
}
