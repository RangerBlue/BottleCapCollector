package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.api.model.response.ValidateItemResponse;
import com.km.bottlecapcollector.cloud.database.item.entity.ItemEntity;
import com.km.bottlecapcollector.cloud.database.item.service.ItemEntityService;
import com.km.bottlecapcollector.cloud.database.mapper.EntityDocumentMapper;
import com.km.bottlecapcollector.cloud.image.ml.api.Embedding;
import com.km.bottlecapcollector.cloud.image.ml.api.EmbeddingService;
import com.km.bottlecapcollector.cloud.storage.CloudStorageService;
import com.km.bottlecapcollector.color.HSBColor;
import com.km.bottlecapcollector.color.HSBColorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for finding similar items based on HSB color and embeddings.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SimilarityService {

    private static final float HSB_COLOR_MARGIN = 0.1f;
    private static final int SIMILAR_ITEMS_LIMIT = 10;

    private final ItemEntityService itemEntityService;
    private final EmbeddingService embeddingService;
    private final CloudStorageService cloudStorageService;
    private final EntityDocumentMapper documentMapper = EntityDocumentMapper.INSTANCE;

    /**
     * Finds similar items for an uploaded image.
     * Uses a two-pass approach:
     * 1. HSB color pre-filtering (cheap, narrows candidates)
     * 2. Embedding cosine similarity (accurate, compares candidates)
     */
    public ValidateItemResponse findSimilarItems(String collectionKey, String userId, MultipartFile file){
        log.info("Finding similar items in collection: {} for user: {}", collectionKey, userId);

        HSBColor hsbColor = HSBColorService.calculateColor(file);
        log.info("Calculated HSB color - H:{}, S:{}, B:{}", hsbColor.getHue(), hsbColor.getSaturation(), hsbColor.getBrightness());

        Embedding uploadedEmbedding = generateEmbeddingIfAvailable(file);

        List<ValidateItemResponse.SimilarItem> similarItems = findSimilarItemsWithEmbeddings(
                collectionKey, userId, hsbColor, uploadedEmbedding);

        log.info("Found {} similar items in collection: {}", similarItems.size(), collectionKey);

        return ValidateItemResponse.builder()
                .similarCaps(similarItems)
                .hasSimilarItems(!similarItems.isEmpty())
                .build();
    }

    private Embedding generateEmbeddingIfAvailable(MultipartFile file){
        if (!embeddingService.isAvailable()) {
            return null;
        }
        Embedding embedding = embeddingService.generateEmbedding(file);
        if (embedding != null) {
            log.info("Generated embedding with {} dimensions for validation", embedding.getDimensions());
        }
        return embedding;
    }

    private List<ValidateItemResponse.SimilarItem> findSimilarItemsWithEmbeddings(
            String collectionKey, String userId, HSBColor hsbColor, Embedding uploadedEmbedding) {

        List<ItemEntity> candidates = findCandidatesByHSBColor(collectionKey, userId, hsbColor, uploadedEmbedding);
        log.debug("HSB pre-filter found {} candidates", candidates.size());

        List<ValidateItemResponse.SimilarItem> similarItems = calculateSimilarityScores(candidates, hsbColor, uploadedEmbedding);

        return sortAndLimitResults(similarItems);
    }

    private List<ItemEntity> findCandidatesByHSBColor(String collectionKey, String userId, HSBColor hsbColor, Embedding uploadedEmbedding) {
        float hueMin = Math.max(0f, hsbColor.getHue() - HSB_COLOR_MARGIN);
        float hueMax = Math.min(1f, hsbColor.getHue() + HSB_COLOR_MARGIN);
        float satMin = Math.max(0f, hsbColor.getSaturation() - HSB_COLOR_MARGIN);
        float satMax = Math.min(1f, hsbColor.getSaturation() + HSB_COLOR_MARGIN);
        float briMin = Math.max(0f, hsbColor.getBrightness() - HSB_COLOR_MARGIN);
        float briMax = Math.min(1f, hsbColor.getBrightness() + HSB_COLOR_MARGIN);

        int candidateLimit = uploadedEmbedding != null ? SIMILAR_ITEMS_LIMIT * 3 : SIMILAR_ITEMS_LIMIT + 1;

        return itemEntityService.findByHSBColorRangeAndUserId(
                collectionKey, hueMin, hueMax, satMin, satMax, briMin, briMax, userId, candidateLimit);
    }

    private List<ValidateItemResponse.SimilarItem> calculateSimilarityScores(
            List<ItemEntity> candidates, HSBColor hsbColor, Embedding uploadedEmbedding) {

        List<ValidateItemResponse.SimilarItem> similarItems = new ArrayList<>();

        for (ItemEntity item : candidates) {
            Double similarityScore = calculateSimilarityScore(item, hsbColor, uploadedEmbedding);
            String imageUrl = generateSignedUrl(item);

            similarItems.add(ValidateItemResponse.SimilarItem.builder()
                    .id(item.getId())
                    .name(item.getName())
                    .imageUrl(imageUrl)
                    .similarityScore(similarityScore)
                    .build());
        }

        return similarItems;
    }

    private Double calculateSimilarityScore(ItemEntity item, HSBColor hsbColor, Embedding uploadedEmbedding) {
        if (hasValidEmbeddings(item, uploadedEmbedding)) {
            Double score = embeddingService.calculateCosineSimilarity(
                    uploadedEmbedding.getVector(),
                    item.getEmbedding().getVector());
            log.trace("Embedding similarity for item {}: {}", item.getId(), score);
            return score;
        }

        HSBColor itemHsbColor = documentMapper.toDomain(item.getImage().getHsbColor());
        Double score = HSBColorService.calculateHSBSimilarity(hsbColor, itemHsbColor);
        log.trace("HSB similarity for item {} (no embedding): {}", item.getId(), score);
        return score;
    }

    private boolean hasValidEmbeddings(ItemEntity item, Embedding uploadedEmbedding) {
        return uploadedEmbedding != null
                && uploadedEmbedding.getVector() != null
                && item.getEmbedding() != null
                && item.getEmbedding().getVector() != null;
    }

    private String generateSignedUrl(ItemEntity item) {
        if (item.getImage() != null && item.getImage().getObjectName() != null) {
            return cloudStorageService.generateSignedUrl(
                    item.getImage().getObjectName());
        }
        return null;
    }

    private List<ValidateItemResponse.SimilarItem> sortAndLimitResults(List<ValidateItemResponse.SimilarItem> items) {
        items.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));

        if (items.size() > SIMILAR_ITEMS_LIMIT) {
            return items.subList(0, SIMILAR_ITEMS_LIMIT);
        }
        return items;
    }
}
