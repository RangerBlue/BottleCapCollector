package com.km.bottlecapcollector.cloud.service;

import com.km.bottlecapcollector.api.model.request.CreateCollectionItemRequest;
import com.km.bottlecapcollector.api.model.request.UpdateCollectionItem;
import com.km.bottlecapcollector.api.model.response.CollectionItemResponse;
import com.km.bottlecapcollector.api.model.response.ValidateItemResponse;
import com.km.bottlecapcollector.api.model.response.CollectionItemSummary;
import com.km.bottlecapcollector.cloud.database.document.ItemEntity;
import com.km.bottlecapcollector.cloud.database.document.ImageAnalysisMetadataEntity;
import com.km.bottlecapcollector.cloud.database.mapper.FirestoreDocumentMapper;
import com.km.bottlecapcollector.cloud.image.ml.api.Embedding;
import com.km.bottlecapcollector.cloud.image.analysis.api.ImageAnalysisMetadata;
import com.km.bottlecapcollector.cloud.image.ml.api.EmbeddingService;
import com.km.bottlecapcollector.cloud.storage.api.StorageImage;
import com.km.bottlecapcollector.cloud.exception.FirestoreDocumentNotFoundException;
import com.km.bottlecapcollector.cloud.mapper.FirestoreBottleCapMapper;
import com.km.bottlecapcollector.cloud.database.repository.FirestoreBottleCapRepository;
import com.km.bottlecapcollector.cloud.storage.CloudStorageService;
import com.km.bottlecapcollector.cloud.image.analysis.vision.VisionApiService;
import com.km.bottlecapcollector.property.AppProperties;
import com.km.bottlecapcollector.color.HSBColor;
import com.km.bottlecapcollector.color.HSBColorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer for managing collection items in Firestore.
 * Handles CRUD operations, Vision API metadata, and Vertex AI embeddings.
 * Supports multiple Firestore collections specified dynamically via collectionName parameter.
 */
@Service
@Slf4j
public class FirestoreBottleCapService {

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int DEFAULT_PAGE_OFFSET = 0;
    private static final float HSB_COLOR_MARGIN = 0.1f;
    private static final int SIMILAR_CAPS_LIMIT = 10;
    private static final double EMBEDDING_SIMILARITY_THRESHOLD = 0.85;
    private static final int SIGNED_URL_DURATION_MINUTES = 15;

    private final FirestoreBottleCapRepository repository;
    private final CloudStorageService cloudStorageService;
    private final VisionApiService visionApiService;
    private final EmbeddingService embeddingService;
    private final FirestoreUserService userService;
    private final SearchTokenService searchTokenService;
    private final FirestoreBottleCapMapper mapper = FirestoreBottleCapMapper.INSTANCE;
    private final FirestoreDocumentMapper documentMapper = FirestoreDocumentMapper.INSTANCE;

    public FirestoreBottleCapService(FirestoreBottleCapRepository repository,
                                     CloudStorageService cloudStorageService,
                                     VisionApiService visionApiService,
                                     EmbeddingService embeddingService,
                                     FirestoreUserService userService,
                                     SearchTokenService searchTokenService) {
        this.repository = repository;
        this.cloudStorageService = cloudStorageService;
        this.visionApiService = visionApiService;
        this.embeddingService = embeddingService;
        this.userService = userService;
        this.searchTokenService = searchTokenService;
    }

    /**
     * Enriches a CollectionItemResponse with a signed URL for the image.
     */
    private CollectionItemResponse enrichWithSignedUrl(CollectionItemResponse response) {
        if (response != null && response.getImage() != null && response.getImage().getObjectName() != null) {
            String signedUrl = cloudStorageService.generateSignedUrl(
                    response.getImage().getObjectName(), SIGNED_URL_DURATION_MINUTES);
            response.getImage().setSignedUrl(signedUrl);
        }
        return response;
    }

    private CollectionItemSummary enrichWithSignedUrl(CollectionItemSummary response) {
        if (response != null) {
            String signedUrl = cloudStorageService.generateSignedUrl(
                    response.getObjectName(), SIGNED_URL_DURATION_MINUTES);
            response.setSignedUrl(signedUrl);
        }
        return response;
    }

    /**
     * Enriches a list of CollectionItemResponse with signed URLs.
     */
    private List<CollectionItemResponse> enrichWithSignedUrls(List<CollectionItemResponse> responses) {
        responses.forEach(this::enrichWithSignedUrl);
        return responses;
    }

    private List<CollectionItemSummary> enrichSummariesWithSignedUrls(List<CollectionItemSummary> responses) {
        responses.forEach(this::enrichWithSignedUrl);
        return responses;
    }

    /**
     * Creates a new collection item in Firestore with full processing.
     * This method assumes the item has been validated as unique.
     * It performs:
     * 1. Creates the document in Firestore
     * 2. Uploads the image to Cloud Storage
     * 3. Calculates HSB color from the image
     * 4. Analyzes the image with Vision API
     * 5. Generates Vertex AI embedding for the image
     * 6. Extracts tags from Vision API metadata
     * 7. Stores all data to the database
     *
     * @param collectionName the Firestore collection name (e.g., "bottle_caps", "post_stamps")
     * @param request the create request with item details
     * @param file the image file to process
     * @return the created collection item DTO with all metadata
     */
    public CollectionItemResponse createCollectionItem(String collectionName, CreateCollectionItemRequest request, MultipartFile file) {
        log.info("Creating new collection item: {} in collection: {}", request.getName(), collectionName);

        // 1. Create initial document to get an ID
        ItemEntity item = ItemEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .tags(request.getTags() != null ? new ArrayList<>(request.getTags()) : new ArrayList<>())
                .customTags(request.getCustomTags())
                .userId(request.getUserId())
                .collectionName(collectionName)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ItemEntity savedItem = repository.save(collectionName, item);
        String itemId = savedItem.getId();
        log.info("Created collection item with id: {}", itemId);

        try {
            // Process image: upload, analyze, generate embeddings
            processAndAttachImage(savedItem, file);

            // Generate search tokens for efficient prefix search
            List<String> searchTokens = searchTokenService.generateTokens(savedItem);
            savedItem.setSearchTokens(searchTokens);
            log.info("Generated {} search tokens for item: {}", searchTokens.size(), itemId);

            // Update timestamp and save all data
            savedItem.setUpdatedAt(Instant.now());
            ItemEntity finalItem = repository.save(collectionName, savedItem);
            log.info("Successfully created and processed collection item with id: {}", itemId);

            // Register collection for the user
            if (request.getUserId() != null && !request.getUserId().isBlank()) {
                userService.addCollectionToUser(request.getUserId(), collectionName);
            }

            return enrichWithSignedUrl(mapper.toDto(finalItem));

        } catch (Exception e) {
            log.error("Error during collection item creation, cleaning up: {}", itemId, e);
            // Clean up on failure
            try {
                if (savedItem.getImage() != null && savedItem.getImage().getObjectName() != null) {
                    cloudStorageService.deleteImage(savedItem.getImage().getObjectName());
                }
                repository.deleteById(collectionName, itemId);
            } catch (Exception cleanupError) {
                log.error("Failed to clean up collection item: {}", itemId, cleanupError);
            }
            throw new RuntimeException("Failed to create collection item: " + e.getMessage(), e);
        }
    }

    /**
     * Processes an image file and attaches all metadata to a collection item.
     * This method performs:
     * 1. Upload to Cloud Storage
     * 2. HSB color calculation
     * 3. Vision API analysis
     * 4. Vertex AI embedding generation
     * 5. Tag extraction from Vision API metadata
     *
     * @param item the collection item to attach image data to
     * @param file the image file to process
     */
    private void processAndAttachImage(ItemEntity item, MultipartFile file) throws IOException {
        String itemId = item.getId();

        // 1. Upload image to Cloud Storage
        StorageImage image = cloudStorageService.uploadImage(file, itemId);
        log.info("Uploaded image to Cloud Storage: {}", image.getObjectName());

        // 2. Calculate HSB color from the image
        HSBColor hsbColor = HSBColorService.calculateColor(file);
        image.setHsbColor(hsbColor);
        item.setImage(documentMapper.toFirestore(image));
        log.info("Calculated HSB color - H:{}, S:{}, B:{}", hsbColor.getHue(), hsbColor.getSaturation(), hsbColor.getBrightness());

        // 3. Analyze image with Vision API
        ImageAnalysisMetadata visionMetadata = visionApiService.analyzeImageFromFile(file);
        item.setVisionMetadata(documentMapper.toFirestore(visionMetadata));
        log.info("Analyzed image with Vision API");

        // 4. Generate Vertex AI embedding for the image
        if (embeddingService.isAvailable()) {
            Embedding embedding = embeddingService.generateEmbedding(file);
            if (embedding != null) {
                item.setEmbedding(documentMapper.toFirestore(embedding));
                log.info("Generated embedding with {} dimensions", embedding.getDimensions());
            }
        } else {
            log.warn("Embedding service not available, skipping embedding generation");
        }

        // 5. Extract and merge tags from Vision API metadata
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

    /**
     * Retrieves a collection item by its ID, verifying user ownership.
     *
     * @param collectionName the Firestore collection name
     * @param id the document ID
     * @param userId the user ID for ownership verification
     * @return the collection item DTO
     * @throws FirestoreDocumentNotFoundException if not found or not owned by user
     */
    public CollectionItemResponse getCollectionItem(String collectionName, String id, String userId) {
        log.trace("Getting item with id: {} from collection: {} for user: {}", id, collectionName, userId);
        ItemEntity item = repository.findByIdAndUserId(collectionName, id, userId)
                .orElseThrow(() -> new FirestoreDocumentNotFoundException(collectionName, id));
        return enrichWithSignedUrl(mapper.toDto(item));
    }

    /**
     * Retrieves items with pagination from a collection.
     *
     * @param collectionName the Firestore collection name
     * @param limit  the maximum number to return
     * @param offset the number to skip
     * @return paginated list of collection item DTOs
     */
    public List<CollectionItemResponse> getCollectionByTypePaginated(String collectionName, int limit, int offset) {
        log.trace("Getting items from collection: {} with limit: {}, offset: {}", collectionName, limit, offset);
        return enrichWithSignedUrls(mapper.toDtoList(repository.findAll(collectionName, limit, offset)));
    }

    /**
     * Retrieves items by user ID from a collection.
     *
     * @param collectionName the Firestore collection name
     * @param userId the user ID
     * @return list of collection item DTOs belonging to the user
     */
    public List<CollectionItemResponse> getItemsByUserId(String collectionName, String userId) {
        log.trace("Getting items for user: {} from collection: {}", userId, collectionName);
        return enrichWithSignedUrls(mapper.toDtoList(repository.findByUserId(collectionName, userId)));
    }

    /**
     * Searches items across multiple fields: name, description, tags, customTags,
     * and Vision API metadata (labels, text, logos).
     *
     * @param collectionName the Firestore collection name
     * @param query the search query (case-insensitive)
     * @return list of matching collection item DTOs
     */
    public List<CollectionItemResponse> searchItems(String collectionName, String query) {
        log.info("Searching items in collection: {} with query: {}", collectionName, query);

        if (query == null || query.isBlank()) {
            return new ArrayList<>();
        }

        String searchTerm = query.toLowerCase().trim();

        // Fetch all items from collection (Firestore doesn't support full-text search)
        List<ItemEntity> allItems = repository.findAll(collectionName);

        List<ItemEntity> matchingItems = allItems.stream()
                .filter(item -> matchesSearchQuery(item, searchTerm))
                .toList();

        log.info("Found {} items matching query: {}", matchingItems.size(), query);
        return enrichWithSignedUrls(mapper.toDtoList(matchingItems));
    }

    /**
     * Searches items with pagination support using Firestore array-contains query.
     * Returns lightweight summary DTOs for efficient list rendering.
     * Results are filtered by userId to ensure users only see their own items.
     * If query is null or blank, returns all user's items paginated.
     * Otherwise uses searchTokens field for efficient prefix search.
     *
     * @param collectionName the Firestore collection name
     * @param query the optional search query (case-insensitive prefix)
     * @param userId the user ID to filter items by
     * @param pageable pagination information
     * @return page of lightweight summary DTOs belonging to the user
     */
    public Page<CollectionItemSummary> searchItemsPaginated(String collectionName, String query, String userId, Pageable pageable) {
        log.info("Searching items in collection: {} with query: '{}' for user: {}, page: {}, size: {}",
                collectionName, query, userId, pageable.getPageNumber(), pageable.getPageSize());

        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();

        List<ItemEntity> items;
        long totalElements;

        if (query == null || query.isBlank()) {
            // No search query - return all user's items with pagination
            items = repository.findByUserId(collectionName, userId, limit, offset);
            totalElements = repository.countByUserId(collectionName, userId);
        } else {
            // Use search token for efficient Firestore query filtered by userId
            String searchToken = query.toLowerCase().trim();
            items = repository.findBySearchTokenAndUserId(collectionName, searchToken, userId, limit, offset);
            totalElements = repository.countBySearchTokenAndUserId(collectionName, searchToken, userId);
        }

        log.info("Returning page {} of {} items (total: {})", pageable.getPageNumber(), items.size(), totalElements);
        List<CollectionItemSummary> content = enrichSummariesWithSignedUrls(mapper.toSummaryList(items));
        return new PageImpl<>(content, pageable, totalElements);
    }

    /**
     * Checks if an item matches the search query across all searchable fields.
     */
    private boolean matchesSearchQuery(ItemEntity item, String searchTerm) {
        // Search in name
        if (item.getName() != null && item.getName().toLowerCase().contains(searchTerm)) {
            return true;
        }

        // Search in description
        if (item.getDescription() != null && item.getDescription().toLowerCase().contains(searchTerm)) {
            return true;
        }

        // Search in tags
        if (item.getTags() != null) {
            for (String tag : item.getTags()) {
                if (tag != null && tag.toLowerCase().contains(searchTerm)) {
                    return true;
                }
            }
        }

        // Search in customTags (both keys and values)
        if (item.getCustomTags() != null) {
            for (var entry : item.getCustomTags().entrySet()) {
                if (entry.getKey() != null && entry.getKey().toLowerCase().contains(searchTerm)) {
                    return true;
                }
                if (entry.getValue() != null && entry.getValue().toLowerCase().contains(searchTerm)) {
                    return true;
                }
            }
        }

        // Search in Vision API metadata
        if (item.getVisionMetadata() != null) {
            ImageAnalysisMetadataEntity metadata = item.getVisionMetadata();

            // Search in labels (description)
            if (metadata.getImageLabels() != null) {
                for (var label : metadata.getImageLabels()) {
                    if (label.getDescription() != null &&
                        label.getDescription().toLowerCase().contains(searchTerm)) {
                        return true;
                    }
                }
            }

            // Search in text annotation (fullText, words, language)
            if (metadata.getTextAnnotation() != null) {
                ImageAnalysisMetadataEntity.FirestoreImageText text = metadata.getTextAnnotation();
                if (text.getFullText() != null && text.getFullText().toLowerCase().contains(searchTerm)) {
                    return true;
                }
                if (text.getLanguage() != null && text.getLanguage().toLowerCase().contains(searchTerm)) {
                    return true;
                }
                if (text.getWords() != null) {
                    for (String word : text.getWords()) {
                        if (word != null && word.toLowerCase().contains(searchTerm)) {
                            return true;
                        }
                    }
                }
            }

            // Search in logo annotation (description)
            if (metadata.getLogoAnnotation() != null) {
                ImageAnalysisMetadataEntity.FirestoreImageLogo logo = metadata.getLogoAnnotation();
                if (logo.getDescription() != null && logo.getDescription().toLowerCase().contains(searchTerm)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Updates a collection item's basic information.
     *
     * @param collectionName the Firestore collection name
     * @param id the document ID
     * @param userId the user ID for ownership verification
     * @param request the update request
     * @return the updated collection item DTO
     */
    public CollectionItemResponse updateItem(String collectionName, String id, String userId, UpdateCollectionItem request) {
        log.info("Updating item with id: {} in collection: {} for user: {}", id, collectionName, userId);

        ItemEntity item = repository.findByIdAndUserId(collectionName, id, userId)
                .orElseThrow(() -> new FirestoreDocumentNotFoundException(collectionName, id));

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
        }

        // Regenerate search tokens if searchable fields changed
        if (needsTokenRegeneration) {
            List<String> searchTokens = searchTokenService.generateTokens(item);
            item.setSearchTokens(searchTokens);
            log.info("Regenerated {} search tokens for item: {}", searchTokens.size(), id);
        }

        item.setUpdatedAt(Instant.now());

        ItemEntity saved = repository.save(collectionName, item);
        log.info("Successfully updated item with id: {}", id);
        return enrichWithSignedUrl(mapper.toDto(saved));
    }

    /**
     * Updates the image for a collection item with full processing.
     * Deletes the old image, uploads the new one, and performs all analysis.
     *
     * @param collectionName the Firestore collection name
     * @param id the document ID
     * @param userId the user ID for ownership verification
     * @param file the new image file
     * @return the updated collection item DTO
     */
    public CollectionItemResponse updateImage(String collectionName, String id, String userId, MultipartFile file) throws IOException {
        log.info("Updating image for item with id: {} in collection: {} for user: {}", id, collectionName, userId);

        ItemEntity item = repository.findByIdAndUserId(collectionName, id, userId)
                .orElseThrow(() -> new FirestoreDocumentNotFoundException(collectionName, id));

        // Delete old image if exists
        if (item.getImage() != null && item.getImage().getObjectName() != null) {
            log.info("Deleting old image: {}", item.getImage().getObjectName());
            cloudStorageService.deleteImage(item.getImage().getObjectName());
        }

        // Process new image: upload, analyze, generate embeddings
        processAndAttachImage(item, file);

        // Regenerate search tokens with new vision metadata
        List<String> searchTokens = searchTokenService.generateTokens(item);
        item.setSearchTokens(searchTokens);
        log.info("Regenerated {} search tokens for item: {}", searchTokens.size(), id);

        item.setUpdatedAt(Instant.now());
        ItemEntity saved = repository.save(collectionName, item);
        log.info("Successfully updated image for item with id: {}", id);
        return enrichWithSignedUrl(mapper.toDto(saved));
    }

    /**
     * Deletes a collection item after verifying user ownership.
     *
     * @param collectionName the Firestore collection name
     * @param id the document ID
     * @param userId the user ID for ownership verification
     */
    public void deleteItem(String collectionName, String id, String userId) {
        log.info("Deleting item with id: {} from collection: {} for user: {}", id, collectionName, userId);

        // Verify ownership at database level before deleting
        if (repository.findByIdAndUserId(collectionName, id, userId).isEmpty()) {
            throw new FirestoreDocumentNotFoundException(collectionName, id);
        }

        repository.deleteById(collectionName, id);
        log.info("Successfully deleted item with id: {}", id);
    }


    /**
     * Validates if a similar item already exists in the collection based on the uploaded image.
     * Uses a two-pass approach:
     * 1. First pass (cheap): HSB color filtering to narrow down candidates
     * 2. Second pass (accurate): Embedding cosine similarity for precise matching
     *
     * @param collectionName the Firestore collection name to search within
     * @param userId the user ID to filter items by
     * @param file the image file to analyze
     * @return CheckCapResponse with list of similar items found
     * @throws IOException if image analysis fails
     */
    public ValidateItemResponse validateItem(String collectionName, String userId, MultipartFile file) throws IOException {
        log.info("Validating if similar item exists in collection: {} for user: {}", collectionName, userId);

        // 1. Calculate HSB color from the uploaded image (cheap pre-filter)
        HSBColor hsbColor = HSBColorService.calculateColor(file);
        log.info("Calculated HSB color - H:{}, S:{}, B:{}", hsbColor.getHue(), hsbColor.getSaturation(), hsbColor.getBrightness());

        // 2. Generate embedding for the uploaded image (for accurate comparison)
        Embedding uploadedEmbedding = null;
        if (embeddingService.isAvailable()) {
            uploadedEmbedding = embeddingService.generateEmbedding(file);
            if (uploadedEmbedding != null) {
                log.info("Generated embedding with {} dimensions for validation", uploadedEmbedding.getDimensions());
            }
        }

        // 3. Find similar items using HSB pre-filter + embedding comparison (filtered by userId)
        List<ValidateItemResponse.SimilarItem> similarItems = findSimilarItemsWithEmbeddings(
                collectionName, userId, hsbColor, uploadedEmbedding);

        log.info("Found {} similar items in collection: {}", similarItems.size(), collectionName);

        return ValidateItemResponse.builder()
                .similarCaps(similarItems)
                .hasSimilarItems(!similarItems.isEmpty())
                .build();
    }

    /**
     * Finds similar items using a two-pass approach:
     * 1. HSB color pre-filtering (cheap, narrows candidates)
     * 2. Embedding cosine similarity (accurate, compares candidates)
     *
     * If embeddings are not available, falls back to HSB-only comparison.
     */
    private List<ValidateItemResponse.SimilarItem> findSimilarItemsWithEmbeddings(
            String collectionName,
            String userId,
            HSBColor hsbColor,
            Embedding uploadedEmbedding) {

        // First pass: HSB color range filtering (filtered by userId at database level)
        float hueMin = Math.max(0f, hsbColor.getHue() - HSB_COLOR_MARGIN);
        float hueMax = Math.min(1f, hsbColor.getHue() + HSB_COLOR_MARGIN);
        float satMin = Math.max(0f, hsbColor.getSaturation() - HSB_COLOR_MARGIN);
        float satMax = Math.min(1f, hsbColor.getSaturation() + HSB_COLOR_MARGIN);
        float briMin = Math.max(0f, hsbColor.getBrightness() - HSB_COLOR_MARGIN);
        float briMax = Math.min(1f, hsbColor.getBrightness() + HSB_COLOR_MARGIN);

        // Get more candidates than needed since we'll filter by embedding
        int candidateLimit = uploadedEmbedding != null ? SIMILAR_CAPS_LIMIT * 3 : SIMILAR_CAPS_LIMIT + 1;

        List<ItemEntity> candidates = repository.findByHSBColorRangeAndUserId(
                collectionName, hueMin, hueMax, satMin, satMax, briMin, briMax, userId, candidateLimit);

        log.debug("HSB pre-filter found {} candidates", candidates.size());

        List<ValidateItemResponse.SimilarItem> similarItems = new ArrayList<>();

        for (ItemEntity item : candidates) {
            Double similarityScore;

            // Second pass: Use embedding similarity if available
            if (uploadedEmbedding != null && uploadedEmbedding.getVector() != null &&
                item.getEmbedding() != null && item.getEmbedding().getVector() != null) {

                similarityScore = embeddingService.calculateCosineSimilarity(
                        uploadedEmbedding.getVector(),
                        item.getEmbedding().getVector());

                log.trace("Embedding similarity for item {}: {}", item.getId(), similarityScore);

            } else {
                // Fallback: Calculate similarity based on HSB distance
                HSBColor itemHsbColor = documentMapper.toDomain(item.getImage().getHsbColor());
                similarityScore = HSBColorService.calculateHSBSimilarity(hsbColor, itemHsbColor);
                log.trace("HSB similarity for item {} (no embedding): {}", item.getId(), similarityScore);
            }

            String imageUrl = null;
            if (item.getImage() != null && item.getImage().getObjectName() != null) {
                imageUrl = cloudStorageService.generateSignedUrl(
                        item.getImage().getObjectName(), SIGNED_URL_DURATION_MINUTES);
            }

            similarItems.add(ValidateItemResponse.SimilarItem.builder()
                    .id(item.getId())
                    .name(item.getName())
                    .imageUrl(imageUrl)
                    .similarityScore(similarityScore)
                    .build());
        }

        // Sort by similarity score (highest first) and limit results
        similarItems.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));

        if (similarItems.size() > SIMILAR_CAPS_LIMIT) {
            similarItems = similarItems.subList(0, SIMILAR_CAPS_LIMIT);
        }

        return similarItems;
    }
}
