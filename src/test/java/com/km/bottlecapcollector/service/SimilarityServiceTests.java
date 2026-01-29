package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.api.model.response.ValidateItemResponse;
import com.km.bottlecapcollector.cloud.database.item.entity.EmbeddingEntity;
import com.km.bottlecapcollector.cloud.database.item.entity.HSBColorEntity;
import com.km.bottlecapcollector.cloud.database.item.entity.ItemEntity;
import com.km.bottlecapcollector.cloud.database.item.entity.StorageImageEntity;
import com.km.bottlecapcollector.cloud.database.item.service.ItemEntityService;
import com.km.bottlecapcollector.cloud.image.ml.api.Embedding;
import com.km.bottlecapcollector.cloud.image.ml.api.EmbeddingService;
import com.km.bottlecapcollector.cloud.storage.CloudStorageService;
import com.km.bottlecapcollector.color.HSBColor;
import com.km.bottlecapcollector.color.HSBColorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SimilarityServiceTests {

    private static final String COLLECTION_KEY = "test-collection";
    private static final String USER_ID = "user-123";
    private static final String SIGNED_URL = "https://storage.googleapis.com/signed-url";

    @Mock
    private ItemEntityService itemEntityService;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private CloudStorageService cloudStorageService;

    @Mock
    private MultipartFile multipartFile;

    private SimilarityService similarityService;

    @BeforeEach
    void setUp() {
        similarityService = new SimilarityService(
                itemEntityService,
                embeddingService,
                cloudStorageService
        );
    }

    @Nested
    class FindSimilarItemsTests {

        @Test
        void findSimilarItems_shouldReturnEmptyWhenNoCandidates() {
            // Given
            HSBColor hsbColor = new HSBColor(0.5f, 0.5f, 0.5f);

            when(embeddingService.isAvailable()).thenReturn(false);
            when(itemEntityService.findByHSBColorRangeAndUserId(
                    anyString(), anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                    anyFloat(), anyFloat(), anyString(), anyInt()))
                    .thenReturn(List.of());

            try (MockedStatic<HSBColorService> mockedHSBColorService = mockStatic(HSBColorService.class)) {
                mockedHSBColorService.when(() -> HSBColorService.calculateColor(multipartFile))
                        .thenReturn(hsbColor);

                // When
                ValidateItemResponse result = similarityService.findSimilarItems(
                        COLLECTION_KEY, USER_ID, multipartFile);

                // Then
                assertNotNull(result);
                assertFalse(result.isHasSimilarItems());
                assertTrue(result.getSimilarCaps().isEmpty());
            }
        }

        @Test
        void findSimilarItems_shouldReturnSimilarItemsUsingHSBWhenEmbeddingUnavailable() {
            // Given
            HSBColor hsbColor = new HSBColor(0.5f, 0.6f, 0.7f);
            ItemEntity candidate = createItemEntity("item-1", "Test Cap", 0.5f, 0.6f, 0.7f, null);

            when(embeddingService.isAvailable()).thenReturn(false);
            when(itemEntityService.findByHSBColorRangeAndUserId(
                    eq(COLLECTION_KEY), anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                    anyFloat(), anyFloat(), eq(USER_ID), anyInt()))
                    .thenReturn(List.of(candidate));
            when(cloudStorageService.generateSignedUrl(anyString())).thenReturn(SIGNED_URL);

            try (MockedStatic<HSBColorService> mockedHSBColorService = mockStatic(HSBColorService.class)) {
                mockedHSBColorService.when(() -> HSBColorService.calculateColor(multipartFile))
                        .thenReturn(hsbColor);
                mockedHSBColorService.when(() -> HSBColorService.calculateHSBSimilarity(any(HSBColor.class), any(HSBColor.class)))
                        .thenReturn(0.95);

                // When
                ValidateItemResponse result = similarityService.findSimilarItems(
                        COLLECTION_KEY, USER_ID, multipartFile);

                // Then
                assertNotNull(result);
                assertTrue(result.isHasSimilarItems());
                assertEquals(1, result.getSimilarCaps().size());
                assertEquals("item-1", result.getSimilarCaps().get(0).getId());
                assertEquals("Test Cap", result.getSimilarCaps().get(0).getName());
                assertEquals(SIGNED_URL, result.getSimilarCaps().get(0).getImageUrl());
                assertEquals(0.95, result.getSimilarCaps().get(0).getSimilarityScore());
            }
        }

        @Test
        void findSimilarItems_shouldUseEmbeddingsWhenAvailable() {
            // Given
            HSBColor hsbColor = new HSBColor(0.5f, 0.6f, 0.7f);
            List<Float> uploadedVector = List.of(0.1f, 0.2f, 0.3f);
            List<Float> itemVector = List.of(0.15f, 0.22f, 0.28f);

            Embedding uploadedEmbedding = Embedding.builder()
                    .vector(uploadedVector)
                    .dimensions(3)
                    .build();

            ItemEntity candidate = createItemEntity("item-1", "Test Cap", 0.5f, 0.6f, 0.7f, itemVector);

            when(embeddingService.isAvailable()).thenReturn(true);
            when(embeddingService.generateEmbedding(multipartFile)).thenReturn(uploadedEmbedding);
            when(embeddingService.calculateCosineSimilarity(uploadedVector, itemVector)).thenReturn(0.98);
            when(itemEntityService.findByHSBColorRangeAndUserId(
                    eq(COLLECTION_KEY), anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                    anyFloat(), anyFloat(), eq(USER_ID), anyInt()))
                    .thenReturn(List.of(candidate));
            when(cloudStorageService.generateSignedUrl(anyString())).thenReturn(SIGNED_URL);

            try (MockedStatic<HSBColorService> mockedHSBColorService = mockStatic(HSBColorService.class)) {
                mockedHSBColorService.when(() -> HSBColorService.calculateColor(multipartFile))
                        .thenReturn(hsbColor);

                // When
                ValidateItemResponse result = similarityService.findSimilarItems(
                        COLLECTION_KEY, USER_ID, multipartFile);

                // Then
                assertNotNull(result);
                assertTrue(result.isHasSimilarItems());
                assertEquals(1, result.getSimilarCaps().size());
                assertEquals(0.98, result.getSimilarCaps().get(0).getSimilarityScore());
                verify(embeddingService).calculateCosineSimilarity(uploadedVector, itemVector);
            }
        }

        @Test
        void findSimilarItems_shouldFallbackToHSBWhenItemHasNoEmbedding() {
            // Given
            HSBColor hsbColor = new HSBColor(0.5f, 0.6f, 0.7f);
            List<Float> uploadedVector = List.of(0.1f, 0.2f, 0.3f);

            Embedding uploadedEmbedding = Embedding.builder()
                    .vector(uploadedVector)
                    .dimensions(3)
                    .build();

            ItemEntity candidateNoEmbedding = createItemEntity("item-1", "Test Cap", 0.5f, 0.6f, 0.7f, null);

            when(embeddingService.isAvailable()).thenReturn(true);
            when(embeddingService.generateEmbedding(multipartFile)).thenReturn(uploadedEmbedding);
            when(itemEntityService.findByHSBColorRangeAndUserId(
                    eq(COLLECTION_KEY), anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                    anyFloat(), anyFloat(), eq(USER_ID), anyInt()))
                    .thenReturn(List.of(candidateNoEmbedding));
            when(cloudStorageService.generateSignedUrl(anyString())).thenReturn(SIGNED_URL);

            try (MockedStatic<HSBColorService> mockedHSBColorService = mockStatic(HSBColorService.class)) {
                mockedHSBColorService.when(() -> HSBColorService.calculateColor(multipartFile))
                        .thenReturn(hsbColor);
                mockedHSBColorService.when(() -> HSBColorService.calculateHSBSimilarity(any(HSBColor.class), any(HSBColor.class)))
                        .thenReturn(0.90);

                // When
                ValidateItemResponse result = similarityService.findSimilarItems(
                        COLLECTION_KEY, USER_ID, multipartFile);

                // Then
                assertEquals(0.90, result.getSimilarCaps().get(0).getSimilarityScore());
                verify(embeddingService, never()).calculateCosineSimilarity(any(), any());
            }
        }

        @Test
        void findSimilarItems_shouldSortResultsByScoreDescending() {
            // Given
            HSBColor hsbColor = new HSBColor(0.5f, 0.6f, 0.7f);

            ItemEntity item1 = createItemEntity("item-1", "Low Score", 0.5f, 0.6f, 0.7f, null);
            ItemEntity item2 = createItemEntity("item-2", "High Score", 0.52f, 0.62f, 0.72f, null);
            ItemEntity item3 = createItemEntity("item-3", "Medium Score", 0.51f, 0.61f, 0.71f, null);

            when(embeddingService.isAvailable()).thenReturn(false);
            when(itemEntityService.findByHSBColorRangeAndUserId(
                    eq(COLLECTION_KEY), anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                    anyFloat(), anyFloat(), eq(USER_ID), anyInt()))
                    .thenReturn(List.of(item1, item2, item3));
            when(cloudStorageService.generateSignedUrl(anyString())).thenReturn(SIGNED_URL);

            try (MockedStatic<HSBColorService> mockedHSBColorService = mockStatic(HSBColorService.class)) {
                mockedHSBColorService.when(() -> HSBColorService.calculateColor(multipartFile))
                        .thenReturn(hsbColor);

                HSBColor hsb1 = new HSBColor(0.5f, 0.6f, 0.7f);
                HSBColor hsb2 = new HSBColor(0.52f, 0.62f, 0.72f);
                HSBColor hsb3 = new HSBColor(0.51f, 0.61f, 0.71f);

                mockedHSBColorService.when(() -> HSBColorService.calculateHSBSimilarity(eq(hsbColor), eq(hsb1)))
                        .thenReturn(0.70);
                mockedHSBColorService.when(() -> HSBColorService.calculateHSBSimilarity(eq(hsbColor), eq(hsb2)))
                        .thenReturn(0.95);
                mockedHSBColorService.when(() -> HSBColorService.calculateHSBSimilarity(eq(hsbColor), eq(hsb3)))
                        .thenReturn(0.85);
                mockedHSBColorService.when(() -> HSBColorService.calculateHSBSimilarity(eq(hsbColor), any(HSBColor.class)))
                        .thenReturn(0.80);

                // When
                ValidateItemResponse result = similarityService.findSimilarItems(
                        COLLECTION_KEY, USER_ID, multipartFile);

                // Then
                assertEquals(3, result.getSimilarCaps().size());
                assertTrue(result.getSimilarCaps().get(0).getSimilarityScore() >=
                           result.getSimilarCaps().get(1).getSimilarityScore());
                assertTrue(result.getSimilarCaps().get(1).getSimilarityScore() >=
                           result.getSimilarCaps().get(2).getSimilarityScore());
            }
        }

        @Test
        void findSimilarItems_shouldLimitResultsTo10Items() {
            // Given
            HSBColor hsbColor = new HSBColor(0.5f, 0.6f, 0.7f);

            List<ItemEntity> candidates = new java.util.ArrayList<>();
            for (int i = 0; i < 15; i++) {
                candidates.add(createItemEntity("item-" + i, "Cap " + i, 0.5f, 0.6f, 0.7f, null));
            }

            when(embeddingService.isAvailable()).thenReturn(false);
            when(itemEntityService.findByHSBColorRangeAndUserId(
                    eq(COLLECTION_KEY), anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                    anyFloat(), anyFloat(), eq(USER_ID), anyInt()))
                    .thenReturn(candidates);
            when(cloudStorageService.generateSignedUrl(anyString())).thenReturn(SIGNED_URL);

            try (MockedStatic<HSBColorService> mockedHSBColorService = mockStatic(HSBColorService.class)) {
                mockedHSBColorService.when(() -> HSBColorService.calculateColor(multipartFile))
                        .thenReturn(hsbColor);
                mockedHSBColorService.when(() -> HSBColorService.calculateHSBSimilarity(any(HSBColor.class), any(HSBColor.class)))
                        .thenReturn(0.80);

                // When
                ValidateItemResponse result = similarityService.findSimilarItems(
                        COLLECTION_KEY, USER_ID, multipartFile);

                // Then
                assertEquals(10, result.getSimilarCaps().size());
            }
        }

        @Test
        void findSimilarItems_shouldHandleItemWithImageButNoObjectName() {
            // Given
            HSBColor hsbColor = new HSBColor(0.5f, 0.6f, 0.7f);

            HSBColorEntity hsbColorEntity = HSBColorEntity.builder()
                    .hue(0.5f)
                    .saturation(0.6f)
                    .brightness(0.7f)
                    .build();

            StorageImageEntity imageWithNoObjectName = StorageImageEntity.builder()
                    .objectName(null)
                    .hsbColor(hsbColorEntity)
                    .build();

            ItemEntity item = ItemEntity.builder()
                    .id("item-1")
                    .name("Item with no object name")
                    .userId(USER_ID)
                    .collectionKey(COLLECTION_KEY)
                    .image(imageWithNoObjectName)
                    .build();

            when(embeddingService.isAvailable()).thenReturn(false);
            when(itemEntityService.findByHSBColorRangeAndUserId(
                    eq(COLLECTION_KEY), anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                    anyFloat(), anyFloat(), eq(USER_ID), anyInt()))
                    .thenReturn(List.of(item));

            try (MockedStatic<HSBColorService> mockedHSBColorService = mockStatic(HSBColorService.class)) {
                mockedHSBColorService.when(() -> HSBColorService.calculateColor(multipartFile))
                        .thenReturn(hsbColor);
                mockedHSBColorService.when(() -> HSBColorService.calculateHSBSimilarity(any(HSBColor.class), any(HSBColor.class)))
                        .thenReturn(0.80);

                // When
                ValidateItemResponse result = similarityService.findSimilarItems(
                        COLLECTION_KEY, USER_ID, multipartFile);

                // Then
                assertEquals(1, result.getSimilarCaps().size());
                assertNull(result.getSimilarCaps().get(0).getImageUrl());
                verify(cloudStorageService, never()).generateSignedUrl(anyString());
            }
        }
    }

    @Nested
    class HSBColorRangeTests {

        @Test
        void findSimilarItems_shouldRequestCorrectCandidateLimitWithEmbeddings() {
            // Given
            HSBColor hsbColor = new HSBColor(0.5f, 0.6f, 0.7f);
            Embedding uploadedEmbedding = Embedding.builder()
                    .vector(List.of(0.1f, 0.2f))
                    .dimensions(2)
                    .build();

            when(embeddingService.isAvailable()).thenReturn(true);
            when(embeddingService.generateEmbedding(multipartFile)).thenReturn(uploadedEmbedding);
            when(itemEntityService.findByHSBColorRangeAndUserId(
                    eq(COLLECTION_KEY), anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                    anyFloat(), anyFloat(), eq(USER_ID), eq(30)))
                    .thenReturn(List.of());

            try (MockedStatic<HSBColorService> mockedHSBColorService = mockStatic(HSBColorService.class)) {
                mockedHSBColorService.when(() -> HSBColorService.calculateColor(multipartFile))
                        .thenReturn(hsbColor);

                // When
                similarityService.findSimilarItems(COLLECTION_KEY, USER_ID, multipartFile);

                // Then - with embeddings, limit should be 10 * 3 = 30
                verify(itemEntityService).findByHSBColorRangeAndUserId(
                        eq(COLLECTION_KEY), anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                        anyFloat(), anyFloat(), eq(USER_ID), eq(30));
            }
        }

        @Test
        void findSimilarItems_shouldRequestCorrectCandidateLimitWithoutEmbeddings() {
            // Given
            HSBColor hsbColor = new HSBColor(0.5f, 0.6f, 0.7f);

            when(embeddingService.isAvailable()).thenReturn(false);
            when(itemEntityService.findByHSBColorRangeAndUserId(
                    eq(COLLECTION_KEY), anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                    anyFloat(), anyFloat(), eq(USER_ID), eq(11)))
                    .thenReturn(List.of());

            try (MockedStatic<HSBColorService> mockedHSBColorService = mockStatic(HSBColorService.class)) {
                mockedHSBColorService.when(() -> HSBColorService.calculateColor(multipartFile))
                        .thenReturn(hsbColor);

                // When
                similarityService.findSimilarItems(COLLECTION_KEY, USER_ID, multipartFile);

                // Then - without embeddings, limit should be 10 + 1 = 11
                verify(itemEntityService).findByHSBColorRangeAndUserId(
                        eq(COLLECTION_KEY), anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                        anyFloat(), anyFloat(), eq(USER_ID), eq(11));
            }
        }

        @Test
        void findSimilarItems_shouldClampHSBRangeAtBoundaries() {
            // Given - color values near boundary (0.0)
            HSBColor hsbColor = new HSBColor(0.05f, 0.05f, 0.05f);

            when(embeddingService.isAvailable()).thenReturn(false);
            when(itemEntityService.findByHSBColorRangeAndUserId(
                    eq(COLLECTION_KEY),
                    eq(0.0f), anyFloat(),  // hueMin clamped to 0
                    eq(0.0f), anyFloat(),  // satMin clamped to 0
                    eq(0.0f), anyFloat(),  // briMin clamped to 0
                    eq(USER_ID), anyInt()))
                    .thenReturn(List.of());

            try (MockedStatic<HSBColorService> mockedHSBColorService = mockStatic(HSBColorService.class)) {
                mockedHSBColorService.when(() -> HSBColorService.calculateColor(multipartFile))
                        .thenReturn(hsbColor);

                // When
                similarityService.findSimilarItems(COLLECTION_KEY, USER_ID, multipartFile);

                // Then - min values should be clamped to 0
                verify(itemEntityService).findByHSBColorRangeAndUserId(
                        eq(COLLECTION_KEY),
                        eq(0.0f), anyFloat(),
                        eq(0.0f), anyFloat(),
                        eq(0.0f), anyFloat(),
                        eq(USER_ID), anyInt());
            }
        }
    }

    private ItemEntity createItemEntity(String id, String name, float hue, float saturation, float brightness, List<Float> embeddingVector) {
        HSBColorEntity hsbColorEntity = HSBColorEntity.builder()
                .hue(hue)
                .saturation(saturation)
                .brightness(brightness)
                .build();

        StorageImageEntity imageEntity = StorageImageEntity.builder()
                .objectName(id + "/image.jpg")
                .hsbColor(hsbColorEntity)
                .build();

        EmbeddingEntity embeddingEntity = null;
        if (embeddingVector != null) {
            embeddingEntity = EmbeddingEntity.builder()
                    .vector(embeddingVector)
                    .dimensions(embeddingVector.size())
                    .build();
        }

        return ItemEntity.builder()
                .id(id)
                .name(name)
                .userId(USER_ID)
                .collectionKey(COLLECTION_KEY)
                .image(imageEntity)
                .embedding(embeddingEntity)
                .build();
    }
}
