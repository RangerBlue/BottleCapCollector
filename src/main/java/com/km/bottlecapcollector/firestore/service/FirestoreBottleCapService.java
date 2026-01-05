package com.km.bottlecapcollector.firestore.service;

import com.km.bottlecapcollector.firestore.document.*;
import com.km.bottlecapcollector.firestore.dto.*;
import com.km.bottlecapcollector.firestore.exception.FirestoreDocumentNotFoundException;
import com.km.bottlecapcollector.firestore.mapper.FirestoreBottleCapMapper;
import com.km.bottlecapcollector.firestore.repository.FirestoreBottleCapRepository;
import com.km.bottlecapcollector.property.AppProperties;
import com.km.bottlecapcollector.util.color.HSBColor;
import com.km.bottlecapcollector.util.color.HSBColorService;
import lombok.extern.slf4j.Slf4j;
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
    private final AppProperties appProperties;
    private final FirestoreBottleCapMapper mapper = FirestoreBottleCapMapper.INSTANCE;

    public FirestoreBottleCapService(FirestoreBottleCapRepository repository,
                                     CloudStorageService cloudStorageService,
                                     VisionApiService visionApiService,
                                     EmbeddingService embeddingService,
                                     FirestoreUserService userService,
                                     AppProperties appProperties) {
        this.repository = repository;
        this.cloudStorageService = cloudStorageService;
        this.visionApiService = visionApiService;
        this.embeddingService = embeddingService;
        this.userService = userService;
        this.appProperties = appProperties;
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

    /**
     * Enriches a list of CollectionItemResponse with signed URLs.
     */
    private List<CollectionItemResponse> enrichWithSignedUrls(List<CollectionItemResponse> responses) {
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
        CollectionItem item = CollectionItem.builder()
                .name(request.getName())
                .description(request.getDescription())
                .tags(request.getTags() != null ? new ArrayList<>(request.getTags()) : new ArrayList<>())
                .customTags(request.getCustomTags())
                .userId(request.getUserId())
                .collectionName(collectionName)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .temporary(false)
                .build();

        CollectionItem savedItem = repository.save(collectionName, item);
        String itemId = savedItem.getId();
        log.info("Created collection item with id: {}", itemId);

        try {
            // Process image: upload, analyze, generate embeddings
            processAndAttachImage(savedItem, file, true);

            // Update timestamp and save all data
            savedItem.setUpdatedAt(Instant.now());
            CollectionItem finalItem = repository.save(collectionName, savedItem);
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
     * @param permanent whether to store in permanent folder (true) or temporary (false)
     */
    private void processAndAttachImage(CollectionItem item, MultipartFile file, boolean permanent) throws IOException {
        String itemId = item.getId();

        // 1. Upload image to Cloud Storage
        FirestoreImage image = cloudStorageService.uploadImage(file, itemId, permanent);
        log.info("Uploaded image to Cloud Storage: {}", image.getObjectName());

        // 2. Calculate HSB color from the image
        HSBColor hsbColor = HSBColorService.calculateColor(file);
        FirestoreHSBColor firestoreHSBColor = FirestoreHSBColor.builder()
                .hue(hsbColor.getHue())
                .saturation(hsbColor.getSaturation())
                .brightness(hsbColor.getBrightness())
                .build();
        image.setHsbColor(firestoreHSBColor);
        item.setImage(image);
        log.info("Calculated HSB color - H:{}, S:{}, B:{}", hsbColor.getHue(), hsbColor.getSaturation(), hsbColor.getBrightness());

        // 3. Analyze image with Vision API
        FirestoreVisionMetadata visionMetadata = visionApiService.analyzeImageFromGcs(image.getStorageUrl());
        item.setVisionMetadata(visionMetadata);
        log.info("Analyzed image with Vision API");

        // 4. Generate Vertex AI embedding for the image
        if (embeddingService.isAvailable()) {
            FirestoreEmbedding embedding = embeddingService.generateEmbedding(file);
            if (embedding != null) {
                item.setEmbedding(embedding);
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
     * Retrieves a collection item by its ID.
     *
     * @param collectionName the Firestore collection name
     * @param id the document ID
     * @return the collection item DTO
     * @throws FirestoreDocumentNotFoundException if not found
     */
    public CollectionItemResponse getCollectionItem(String collectionName, String id) {
        log.trace("Getting item with id: {} from collection: {}", id, collectionName);
        CollectionItem item = repository.findById(collectionName, id)
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
        List<CollectionItem> allItems = repository.findAll(collectionName);

        List<CollectionItem> matchingItems = allItems.stream()
                .filter(item -> matchesSearchQuery(item, searchTerm))
                .toList();

        log.info("Found {} items matching query: {}", matchingItems.size(), query);
        return enrichWithSignedUrls(mapper.toDtoList(matchingItems));
    }

    /**
     * Checks if an item matches the search query across all searchable fields.
     */
    private boolean matchesSearchQuery(CollectionItem item, String searchTerm) {
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
            FirestoreVisionMetadata metadata = item.getVisionMetadata();

            // Search in labels (description)
            if (metadata.getLabels() != null) {
                for (var label : metadata.getLabels()) {
                    if (label.getDescription() != null &&
                        label.getDescription().toLowerCase().contains(searchTerm)) {
                        return true;
                    }
                }
            }

            // Search in text annotation (fullText, words, language)
            if (metadata.getTextAnnotation() != null) {
                FirestoreVisionMetadata.VisionText text = metadata.getTextAnnotation();
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
                FirestoreVisionMetadata.VisionLogo logo = metadata.getLogoAnnotation();
                if (logo.getDescription() != null && logo.getDescription().toLowerCase().contains(searchTerm)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Retrieves items by tag from a collection.
     *
     * @param collectionName the Firestore collection name
     * @param tag the tag to filter by
     * @return list of collection item DTOs with the specified tag
     */
    public List<CollectionItemResponse> getItemsByTag(String collectionName, String tag) {
        log.trace("Getting items by tag: {} from collection: {}", tag, collectionName);
        return enrichWithSignedUrls(mapper.toDtoList(repository.findByTag(collectionName, tag)));
    }

    /**
     * Updates a collection item's basic information.
     *
     * @param collectionName the Firestore collection name
     * @param id the document ID
     * @param request the update request
     * @return the updated collection item DTO
     */
    public CollectionItemResponse updateItem(String collectionName, String id, UpdateCollectionItem request) {
        log.info("Updating item with id: {} in collection: {}", id, collectionName);

        CollectionItem item = repository.findById(collectionName, id)
                .orElseThrow(() -> new FirestoreDocumentNotFoundException(collectionName, id));

        if (request.getName() != null) {
            item.setName(request.getName());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getTags() != null) {
            item.setTags(request.getTags());
        }
        if (request.getCustomTags() != null) {
            item.setCustomTags(request.getCustomTags());
        }
        item.setUpdatedAt(Instant.now());

        CollectionItem saved = repository.save(collectionName, item);
        log.info("Successfully updated item with id: {}", id);
        return enrichWithSignedUrl(mapper.toDto(saved));
    }

    /**
     * Updates the image for a collection item with full processing.
     * Deletes the old image, uploads the new one, and performs all analysis.
     *
     * @param collectionName the Firestore collection name
     * @param id the document ID
     * @param file the new image file
     * @return the updated collection item DTO
     */
    public CollectionItemResponse updateImage(String collectionName, String id, MultipartFile file) throws IOException {
        log.info("Updating image for item with id: {} in collection: {}", id, collectionName);

        CollectionItem item = repository.findById(collectionName, id)
                .orElseThrow(() -> new FirestoreDocumentNotFoundException(collectionName, id));

        // Delete old image if exists
        if (item.getImage() != null && item.getImage().getObjectName() != null) {
            log.info("Deleting old image: {}", item.getImage().getObjectName());
            cloudStorageService.deleteImage(item.getImage().getObjectName());
        }

        // Process new image: upload, analyze, generate embeddings
        boolean isPermanent = !Boolean.TRUE.equals(item.getTemporary());
        processAndAttachImage(item, file, isPermanent);

        item.setUpdatedAt(Instant.now());
        CollectionItem saved = repository.save(collectionName, item);
        log.info("Successfully updated image for item with id: {}", id);
        return enrichWithSignedUrl(mapper.toDto(saved));
    }

    /**
     * Uploads an image file to Cloud Storage and associates it with a collection item.
     *
     * @param collectionName the Firestore collection name
     * @param id   the document ID
     * @param file the image file to upload
     * @return the updated collection item DTO with image metadata
     * @throws IOException if upload fails
     */
    public CollectionItemResponse uploadImage(String collectionName, String id, MultipartFile file) throws IOException {
        log.info("Uploading image for item with id: {} in collection: {}", id, collectionName);

        CollectionItem item = repository.findById(collectionName, id)
                .orElseThrow(() -> new FirestoreDocumentNotFoundException(collectionName, id));

        // Delete old image if exists
        if (item.getImage() != null && item.getImage().getObjectName() != null) {
            log.info("Deleting old image: {}", item.getImage().getObjectName());
            cloudStorageService.deleteImage(item.getImage().getObjectName());
        }

        // Upload new image
        FirestoreImage image = cloudStorageService.uploadImage(file, id);
        item.setImage(image);
        item.setUpdatedAt(Instant.now());

        CollectionItem saved = repository.save(collectionName, item);
        log.info("Successfully uploaded and saved image for item with id: {}", id);
        return enrichWithSignedUrl(mapper.toDto(saved));
    }

    /**
     * Updates or sets the Vision API metadata for a collection item.
     *
     * @param collectionName the Firestore collection name
     * @param id          the document ID
     * @param metadataDto the vision metadata
     * @return the updated collection item DTO
     */
    public CollectionItemResponse updateVisionMetadata(String collectionName, String id, FirestoreVisionMetadataDto metadataDto) {
        log.info("Updating vision metadata for item with id: {} in collection: {}", id, collectionName);

        CollectionItem item = repository.findById(collectionName, id)
                .orElseThrow(() -> new FirestoreDocumentNotFoundException(collectionName, id));

        FirestoreVisionMetadata metadata = mapper.toVisionMetadataDocument(metadataDto);
        if (metadata.getAnalyzedAt() == null) {
            metadata.setAnalyzedAt(Instant.now());
        }
        item.setVisionMetadata(metadata);
        item.setUpdatedAt(Instant.now());

        CollectionItem saved = repository.save(collectionName, item);
        log.info("Successfully updated vision metadata for item with id: {}", id);
        return enrichWithSignedUrl(mapper.toDto(saved));
    }

    /**
     * Analyzes a collection item's image using Vision API and updates metadata and tags.
     *
     * @param collectionName the Firestore collection name
     * @param id the document ID
     * @return the updated collection item DTO with vision metadata and tags
     */
    public CollectionItemResponse analyzeImage(String collectionName, String id) {
        log.info("Analyzing image for item with id: {} in collection: {}", id, collectionName);

        CollectionItem item = repository.findById(collectionName, id)
                .orElseThrow(() -> new FirestoreDocumentNotFoundException(collectionName, id));

        if (item.getImage() == null || item.getImage().getObjectName() == null) {
            throw new IllegalStateException("Item has no image to analyze");
        }

        // Read image bytes from Cloud Storage and analyze
        byte[] imageBytes = cloudStorageService.downloadImage(item.getImage().getObjectName());
        FirestoreVisionMetadata metadata = visionApiService.analyzeImageFromBytes(imageBytes);

        // Extract tags from vision metadata
        List<String> extractedTags = visionApiService.extractTags(metadata);

        // Merge with existing tags (avoid duplicates)
        List<String> existingTags = item.getTags();
        if (existingTags != null) {
            for (String tag : extractedTags) {
                if (!existingTags.contains(tag)) {
                    existingTags.add(tag);
                }
            }
        } else {
            item.setTags(extractedTags);
        }

        item.setVisionMetadata(metadata);
        item.setUpdatedAt(Instant.now());

        CollectionItem saved = repository.save(collectionName, item);
        log.info("Successfully analyzed image and updated item with id: {}", id);
        return enrichWithSignedUrl(mapper.toDto(saved));
    }

    /**
     * Uploads an image and immediately analyzes it with Vision API.
     *
     * @param collectionName the Firestore collection name
     * @param id   the document ID
     * @param file the image file to upload and analyze
     * @return the updated collection item DTO with image, vision metadata, and tags
     * @throws IOException if upload fails
     */
    public CollectionItemResponse uploadAndAnalyzeImage(String collectionName, String id, MultipartFile file) throws IOException {
        log.info("Uploading and analyzing image for item with id: {} in collection: {}", id, collectionName);

        // First upload the image
        uploadImage(collectionName, id, file);

        // Then analyze it
        return analyzeImage(collectionName, id);
    }

    /**
     * Deletes a collection item.
     *
     * @param collectionName the Firestore collection name
     * @param id the document ID
     */
    public void deleteItem(String collectionName, String id) {
        log.info("Deleting item with id: {} from collection: {}", id, collectionName);
        if (!repository.existsById(collectionName, id)) {
            throw new FirestoreDocumentNotFoundException(collectionName, id);
        }
        repository.deleteById(collectionName, id);
        log.info("Successfully deleted item with id: {}", id);
    }

    /**
     * Gets the total count of items in a collection.
     *
     * @param collectionName the Firestore collection name
     * @return the total count
     */
    public long getItemCount(String collectionName) {
        log.trace("Counting items in collection: {}", collectionName);
        return repository.count(collectionName);
    }

    /**
     * Validates if a similar item already exists in the collection based on the uploaded image.
     * Uses a two-pass approach:
     * 1. First pass (cheap): HSB color filtering to narrow down candidates
     * 2. Second pass (accurate): Embedding cosine similarity for precise matching
     *
     * @param collectionName the Firestore collection name to search within
     * @param file the image file to analyze
     * @return CheckCapResponse with list of similar items found
     * @throws IOException if image analysis fails
     */
    public CheckCapResponse validateItem(String collectionName, MultipartFile file) throws IOException {
        log.info("Validating if similar item exists in collection: {}", collectionName);

        // 1. Calculate HSB color from the uploaded image (cheap pre-filter)
        HSBColor hsbColor = HSBColorService.calculateColor(file);
        log.info("Calculated HSB color - H:{}, S:{}, B:{}", hsbColor.getHue(), hsbColor.getSaturation(), hsbColor.getBrightness());

        // 2. Generate embedding for the uploaded image (for accurate comparison)
        FirestoreEmbedding uploadedEmbedding = null;
        if (embeddingService.isAvailable()) {
            uploadedEmbedding = embeddingService.generateEmbedding(file);
            if (uploadedEmbedding != null) {
                log.info("Generated embedding with {} dimensions for validation", uploadedEmbedding.getDimensions());
            }
        }

        // 3. Find similar items using HSB pre-filter + embedding comparison
        List<CheckCapResponse.SimilarCapDto> similarItems = findSimilarItemsWithEmbeddings(
                collectionName, hsbColor, uploadedEmbedding, null);

        log.info("Found {} similar items in collection: {}", similarItems.size(), collectionName);

        return CheckCapResponse.builder()
                .similarCaps(similarItems)
                .hasSimilarCaps(!similarItems.isEmpty())
                .build();
    }

    /**
     * Finds similar items using a two-pass approach:
     * 1. HSB color pre-filtering (cheap, narrows candidates)
     * 2. Embedding cosine similarity (accurate, compares candidates)
     *
     * If embeddings are not available, falls back to HSB-only comparison.
     */
    private List<CheckCapResponse.SimilarCapDto> findSimilarItemsWithEmbeddings(
            String collectionName,
            HSBColor hsbColor,
            FirestoreEmbedding uploadedEmbedding,
            String excludeItemId) {

        // First pass: HSB color range filtering
        float hueMin = Math.max(0f, hsbColor.getHue() - HSB_COLOR_MARGIN);
        float hueMax = Math.min(1f, hsbColor.getHue() + HSB_COLOR_MARGIN);
        float satMin = Math.max(0f, hsbColor.getSaturation() - HSB_COLOR_MARGIN);
        float satMax = Math.min(1f, hsbColor.getSaturation() + HSB_COLOR_MARGIN);
        float briMin = Math.max(0f, hsbColor.getBrightness() - HSB_COLOR_MARGIN);
        float briMax = Math.min(1f, hsbColor.getBrightness() + HSB_COLOR_MARGIN);

        // Get more candidates than needed since we'll filter by embedding
        int candidateLimit = uploadedEmbedding != null ? SIMILAR_CAPS_LIMIT * 3 : SIMILAR_CAPS_LIMIT + 1;

        List<CollectionItem> candidates = repository.findByHSBColorRange(
                collectionName, hueMin, hueMax, satMin, satMax, briMin, briMax, candidateLimit);

        log.debug("HSB pre-filter found {} candidates", candidates.size());

        List<CheckCapResponse.SimilarCapDto> similarItems = new ArrayList<>();

        for (CollectionItem item : candidates) {
            // Exclude temporary items and optionally the current item being checked
            if (Boolean.TRUE.equals(item.getTemporary())) {
                continue;
            }
            if (excludeItemId != null && item.getId().equals(excludeItemId)) {
                continue;
            }

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
                similarityScore = calculateHSBSimilarity(hsbColor, item.getImage().getHsbColor());
                log.trace("HSB similarity for item {} (no embedding): {}", item.getId(), similarityScore);
            }

            String imageUrl = null;
            if (item.getImage() != null && item.getImage().getObjectName() != null) {
                imageUrl = cloudStorageService.generateSignedUrl(
                        item.getImage().getObjectName(), SIGNED_URL_DURATION_MINUTES);
            }

            similarItems.add(CheckCapResponse.SimilarCapDto.builder()
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

    /**
     * Calculates similarity score between two HSB colors (0.0 = different, 1.0 = identical).
     */
    private Double calculateHSBSimilarity(HSBColor color1, FirestoreHSBColor color2) {
        if (color2 == null || color2.getHue() == null) {
            return 0.0;
        }

        double hueDiff = Math.abs(color1.getHue() - color2.getHue());
        double satDiff = Math.abs(color1.getSaturation() - color2.getSaturation());
        double briDiff = Math.abs(color1.getBrightness() - color2.getBrightness());

        // Hue is circular, so take the shorter distance
        if (hueDiff > 0.5) {
            hueDiff = 1.0 - hueDiff;
        }

        // Weighted average (hue is most important for color perception)
        double distance = (hueDiff * 0.5) + (satDiff * 0.25) + (briDiff * 0.25);

        // Convert distance to similarity (0 distance = 1.0 similarity)
        return Math.max(0.0, 1.0 - (distance * 2));
    }

    /**
     * Acknowledges a temporary item, making it permanent.
     * Moves the image from temporary to permanent folder and updates the item's temporary flag.
     *
     * @param collectionName the Firestore collection name
     * @param id the temporary item ID
     * @param name optional new name for the item
     * @param description optional new description for the item
     * @return the updated collection item DTO
     */
    public CollectionItemResponse acknowledgeItem(String collectionName, String id, String name, String description) {
        log.info("Acknowledging temporary item with id: {} in collection: {}", id, collectionName);

        CollectionItem item = repository.findById(collectionName, id)
                .orElseThrow(() -> new FirestoreDocumentNotFoundException(collectionName, id));

        if (!Boolean.TRUE.equals(item.getTemporary())) {
            log.warn("Item {} is not temporary, nothing to acknowledge", id);
            return enrichWithSignedUrl(mapper.toDto(item));
        }

        // Move image from temporary to permanent folder
        if (item.getImage() != null && item.getImage().getObjectName() != null) {
            FirestoreImage movedImage = cloudStorageService.moveToPermament(
                    item.getImage().getObjectName(), id);
            // Preserve HSB color from original image
            movedImage.setHsbColor(item.getImage().getHsbColor());
            movedImage.setWidth(item.getImage().getWidth());
            movedImage.setHeight(item.getImage().getHeight());
            item.setImage(movedImage);
        }

        // Update name and description if provided
        if (name != null && !name.isBlank()) {
            item.setName(name);
        }
        if (description != null && !description.isBlank()) {
            item.setDescription(description);
        }

        // Mark as permanent
        item.setTemporary(false);
        item.setUpdatedAt(Instant.now());

        CollectionItem saved = repository.save(collectionName, item);
        log.info("Successfully acknowledged item with id: {}", id);
        return enrichWithSignedUrl(mapper.toDto(saved));
    }

    /**
     * Deletes a temporary item and its associated image.
     * Use this to cancel an item check operation.
     *
     * @param collectionName the Firestore collection name
     * @param id the temporary item ID
     */
    public void discardTemporaryItem(String collectionName, String id) {
        log.info("Discarding temporary item with id: {} from collection: {}", id, collectionName);

        CollectionItem item = repository.findById(collectionName, id)
                .orElseThrow(() -> new FirestoreDocumentNotFoundException(collectionName, id));

        if (!Boolean.TRUE.equals(item.getTemporary())) {
            throw new IllegalStateException("Cannot discard non-temporary item: " + id);
        }

        // Delete image from Cloud Storage
        if (item.getImage() != null && item.getImage().getObjectName() != null) {
            cloudStorageService.deleteImage(item.getImage().getObjectName());
        }

        // Delete document
        repository.deleteById(collectionName, id);
        log.info("Successfully discarded temporary item with id: {}", id);
    }
}
