package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.api.model.request.CreateCollectionItemRequest;
import com.km.bottlecapcollector.api.model.request.UpdateCollectionItem;
import com.km.bottlecapcollector.api.model.response.CollectionItemResponse;
import com.km.bottlecapcollector.api.model.response.CollectionItemSummary;
import com.km.bottlecapcollector.api.model.response.ValidateItemResponse;
import com.km.bottlecapcollector.cloud.database.item.entity.ItemEntity;
import com.km.bottlecapcollector.cloud.database.item.entity.StorageImageEntity;
import com.km.bottlecapcollector.cloud.database.item.service.ItemEntityService;
import com.km.bottlecapcollector.cloud.image.analysis.api.ImageAnalysisMetadata;
import com.km.bottlecapcollector.cloud.image.analysis.vision.VisionApiService;
import com.km.bottlecapcollector.cloud.image.ml.api.Embedding;
import com.km.bottlecapcollector.cloud.image.ml.api.EmbeddingService;
import com.km.bottlecapcollector.cloud.service.SearchTokenService;
import com.km.bottlecapcollector.cloud.storage.CloudStorageService;
import com.km.bottlecapcollector.cloud.storage.api.StorageImage;
import com.km.bottlecapcollector.color.HSBColor;
import com.km.bottlecapcollector.property.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CollectionServiceTests {

    private static final String COLLECTION_KEY = "test-collection-key";
    private static final String USER_ID = "user-123";
    private static final String ITEM_ID = "item-456";
    private static final String COLLECTION_NAME = "My Collection";
    private static final String SIGNED_URL = "https://storage.googleapis.com/signed-url";
    private static final String OBJECT_NAME = "item-456/image.jpg";

    @Mock
    private ItemEntityService itemEntityService;

    @Mock
    private UserService userService;

    @Mock
    private CloudStorageService cloudStorageService;

    @Mock
    private VisionApiService visionApiService;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private SearchTokenService searchTokenService;

    @Mock
    private SimilarityService similarityService;

    @Mock
    private AppProperties appProperties;

    @Mock
    private MultipartFile multipartFile;

    private CollectionService collectionService;

    @BeforeEach
    void setUp() {
        when(appProperties.getMaxItemsPerUser()).thenReturn(100);
        when(itemEntityService.countAllByUserId(USER_ID)).thenReturn(0L);

        collectionService = new CollectionService(
                itemEntityService,
                userService,
                cloudStorageService,
                visionApiService,
                embeddingService,
                searchTokenService,
                similarityService,
                appProperties
        );
    }

    @Test
    void getCollectionItem_shouldReturnItemWithSignedUrl() {
        // Given
        StorageImageEntity imageEntity = StorageImageEntity.builder()
                .objectName(OBJECT_NAME)
                .build();
        ItemEntity item = ItemEntity.builder()
                .id(ITEM_ID)
                .name("Test Cap")
                .userId(USER_ID)
                .collectionKey(COLLECTION_KEY)
                .image(imageEntity)
                .build();

        when(itemEntityService.findByIdAndUserIdOrThrow(COLLECTION_KEY, ITEM_ID, USER_ID)).thenReturn(item);
        when(userService.getCollectionName(USER_ID, COLLECTION_KEY)).thenReturn(COLLECTION_NAME);
        when(cloudStorageService.generateSignedUrl(OBJECT_NAME)).thenReturn(SIGNED_URL);

        // When
        CollectionItemResponse result = collectionService.getCollectionItem(COLLECTION_KEY, ITEM_ID, USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(ITEM_ID, result.getId());
        assertEquals("Test Cap", result.getName());
        assertEquals(COLLECTION_NAME, result.getCollectionName());
        assertNotNull(result.getImage());
        assertEquals(SIGNED_URL, result.getImage().getSignedUrl());
    }

    @Test
    void getCollectionItem_shouldHandleItemWithoutImage() {
        // Given
        ItemEntity item = ItemEntity.builder()
                .id(ITEM_ID)
                .name("Test Cap")
                .userId(USER_ID)
                .collectionKey(COLLECTION_KEY)
                .image(null)
                .build();

        when(itemEntityService.findByIdAndUserIdOrThrow(COLLECTION_KEY, ITEM_ID, USER_ID)).thenReturn(item);
        when(userService.getCollectionName(USER_ID, COLLECTION_KEY)).thenReturn(COLLECTION_NAME);

        // When
        CollectionItemResponse result = collectionService.getCollectionItem(COLLECTION_KEY, ITEM_ID, USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(ITEM_ID, result.getId());
        verify(cloudStorageService, never()).generateSignedUrl(anyString());
    }

    @Test
    void searchItemsPaginated_shouldReturnPageWithoutQuery() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<ItemEntity> items = List.of(
                createItemEntity("item-1", "Cap 1"),
                createItemEntity("item-2", "Cap 2")
        );

        when(itemEntityService.findByUserId(COLLECTION_KEY, USER_ID, 10, 0)).thenReturn(items);
        when(itemEntityService.countByUserId(COLLECTION_KEY, USER_ID)).thenReturn(2L);
        when(cloudStorageService.generateSignedUrl(anyString())).thenReturn(SIGNED_URL);

        // When
        Page<CollectionItemSummary> result = collectionService.searchItemsPaginated(COLLECTION_KEY, null, USER_ID, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        verify(itemEntityService).findByUserId(COLLECTION_KEY, USER_ID, 10, 0);
        verify(itemEntityService, never()).findBySearchTokenAndUserId(anyString(), anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void searchItemsPaginated_shouldReturnPageWithQuery() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        String query = "BEER";
        List<ItemEntity> items = List.of(createItemEntity("item-1", "Beer Cap"));

        when(itemEntityService.findBySearchTokenAndUserId(COLLECTION_KEY, "beer", USER_ID, 10, 0)).thenReturn(items);
        when(itemEntityService.countBySearchTokenAndUserId(COLLECTION_KEY, "beer", USER_ID)).thenReturn(1L);
        when(cloudStorageService.generateSignedUrl(anyString())).thenReturn(SIGNED_URL);

        // When
        Page<CollectionItemSummary> result = collectionService.searchItemsPaginated(COLLECTION_KEY, query, USER_ID, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        verify(itemEntityService).findBySearchTokenAndUserId(COLLECTION_KEY, "beer", USER_ID, 10, 0);
    }

    @Test
    void searchItemsPaginated_shouldTrimAndLowercaseQuery() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        String query = "  COCA COLA  ";

        when(itemEntityService.findBySearchTokenAndUserId(eq(COLLECTION_KEY), eq("coca cola"), eq(USER_ID), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(itemEntityService.countBySearchTokenAndUserId(COLLECTION_KEY, "coca cola", USER_ID)).thenReturn(0L);

        // When
        collectionService.searchItemsPaginated(COLLECTION_KEY, query, USER_ID, pageable);

        // Then
        verify(itemEntityService).findBySearchTokenAndUserId(COLLECTION_KEY, "coca cola", USER_ID, 10, 0);
    }

    @Test
    void updateItem_shouldUpdateNameAndRegenerateTokens() {
        // Given
        ItemEntity existingItem = createItemEntity(ITEM_ID, "Old Name");
        UpdateCollectionItem request = UpdateCollectionItem.builder()
                .name("New Name")
                .build();

        when(itemEntityService.findByIdAndUserIdOrThrow(COLLECTION_KEY, ITEM_ID, USER_ID)).thenReturn(existingItem);
        when(searchTokenService.generateTokens(any(ItemEntity.class))).thenReturn(List.of("new", "name"));
        when(itemEntityService.save(eq(COLLECTION_KEY), any(ItemEntity.class))).thenAnswer(inv -> inv.getArgument(1));
        when(userService.getCollectionName(USER_ID, COLLECTION_KEY)).thenReturn(COLLECTION_NAME);
        when(cloudStorageService.generateSignedUrl(anyString())).thenReturn(SIGNED_URL);

        // When
        CollectionItemResponse result = collectionService.updateItem(COLLECTION_KEY, ITEM_ID, USER_ID, request);

        // Then
        assertNotNull(result);
        assertEquals("New Name", result.getName());
        verify(searchTokenService).generateTokens(any(ItemEntity.class));
    }

    @Test
    void updateItem_shouldUpdateDescriptionWithoutRegeneratingTokens() {
        // Given
        ItemEntity existingItem = createItemEntity(ITEM_ID, "Name");
        UpdateCollectionItem request = UpdateCollectionItem.builder()
                .description("New Description")
                .build();

        when(itemEntityService.findByIdAndUserIdOrThrow(COLLECTION_KEY, ITEM_ID, USER_ID)).thenReturn(existingItem);
        when(itemEntityService.save(eq(COLLECTION_KEY), any(ItemEntity.class))).thenAnswer(inv -> inv.getArgument(1));
        when(userService.getCollectionName(USER_ID, COLLECTION_KEY)).thenReturn(COLLECTION_NAME);
        when(cloudStorageService.generateSignedUrl(anyString())).thenReturn(SIGNED_URL);

        // When
        CollectionItemResponse result = collectionService.updateItem(COLLECTION_KEY, ITEM_ID, USER_ID, request);

        // Then
        assertNotNull(result);
        assertEquals("New Description", result.getDescription());
        verify(searchTokenService, never()).generateTokens(any(ItemEntity.class));
    }

    @Test
    void updateItem_shouldUpdateCustomTagsAndRegenerateTokens() {
        // Given
        ItemEntity existingItem = createItemEntity(ITEM_ID, "Name");
        Map<String, String> customTags = Map.of("brand", "Pepsi");
        UpdateCollectionItem request = UpdateCollectionItem.builder()
                .customTags(customTags)
                .build();

        when(itemEntityService.findByIdAndUserIdOrThrow(COLLECTION_KEY, ITEM_ID, USER_ID)).thenReturn(existingItem);
        when(searchTokenService.generateTokens(any(ItemEntity.class))).thenReturn(List.of("pepsi"));
        when(itemEntityService.save(eq(COLLECTION_KEY), any(ItemEntity.class))).thenAnswer(inv -> inv.getArgument(1));
        when(userService.getCollectionName(USER_ID, COLLECTION_KEY)).thenReturn(COLLECTION_NAME);
        when(cloudStorageService.generateSignedUrl(anyString())).thenReturn(SIGNED_URL);

        // When
        CollectionItemResponse result = collectionService.updateItem(COLLECTION_KEY, ITEM_ID, USER_ID, request);

        // Then
        assertNotNull(result);
        assertEquals(customTags, result.getCustomTags());
        verify(searchTokenService).generateTokens(any(ItemEntity.class));
    }

    @Test
    void deleteItem_shouldDeleteItemAndImage() {
        // Given
        StorageImageEntity imageEntity = StorageImageEntity.builder()
                .objectName(OBJECT_NAME)
                .build();
        ItemEntity item = ItemEntity.builder()
                .id(ITEM_ID)
                .image(imageEntity)
                .build();

        when(itemEntityService.findByIdAndUserIdOrThrow(COLLECTION_KEY, ITEM_ID, USER_ID)).thenReturn(item);

        // When
        collectionService.deleteItem(COLLECTION_KEY, ITEM_ID, USER_ID);

        // Then
        verify(itemEntityService).deleteById(COLLECTION_KEY, ITEM_ID);
        verify(cloudStorageService).deleteImage(OBJECT_NAME);
    }

    @Test
    void deleteItem_shouldDeleteItemWithoutImageWhenNoImage() {
        // Given
        ItemEntity item = ItemEntity.builder()
                .id(ITEM_ID)
                .image(null)
                .build();

        when(itemEntityService.findByIdAndUserIdOrThrow(COLLECTION_KEY, ITEM_ID, USER_ID)).thenReturn(item);

        // When
        collectionService.deleteItem(COLLECTION_KEY, ITEM_ID, USER_ID);

        // Then
        verify(itemEntityService).deleteById(COLLECTION_KEY, ITEM_ID);
        verify(cloudStorageService, never()).deleteImage(anyString());
    }

    @Test
    void deleteItem_shouldContinueWhenImageDeletionFails() {
        // Given
        StorageImageEntity imageEntity = StorageImageEntity.builder()
                .objectName(OBJECT_NAME)
                .build();
        ItemEntity item = ItemEntity.builder()
                .id(ITEM_ID)
                .image(imageEntity)
                .build();

        when(itemEntityService.findByIdAndUserIdOrThrow(COLLECTION_KEY, ITEM_ID, USER_ID)).thenReturn(item);
        doThrow(new RuntimeException("Storage error")).when(cloudStorageService).deleteImage(OBJECT_NAME);

        // When - should not throw
        collectionService.deleteItem(COLLECTION_KEY, ITEM_ID, USER_ID);

        // Then
        verify(itemEntityService).deleteById(COLLECTION_KEY, ITEM_ID);
        verify(cloudStorageService).deleteImage(OBJECT_NAME);
    }

    @Test
    void validateItem_shouldDelegateToSimilarityService() throws IOException {
        // Given
        ValidateItemResponse expectedResponse = ValidateItemResponse.builder()
                .hasSimilarItems(true)
                .similarCaps(List.of())
                .build();

        when(similarityService.findSimilarItems(COLLECTION_KEY, USER_ID, multipartFile)).thenReturn(expectedResponse);

        // When
        ValidateItemResponse result = collectionService.validateItem(COLLECTION_KEY, USER_ID, multipartFile);

        // Then
        assertEquals(expectedResponse, result);
        verify(similarityService).findSimilarItems(COLLECTION_KEY, USER_ID, multipartFile);
    }

    @Test
    void createCollectionItem_shouldCreateItemWithAllProcessing() throws IOException {
        // Given
        CreateCollectionItemRequest request = CreateCollectionItemRequest.builder()
                .name("New Cap")
                .description("A nice cap")
                .tags(List.of("beer"))
                .collectionName(COLLECTION_NAME)
                .build();

        ItemEntity savedItem = ItemEntity.builder()
                .id(ITEM_ID)
                .name("New Cap")
                .userId(USER_ID)
                .collectionKey(COLLECTION_KEY)
                .tags(new ArrayList<>(List.of("beer")))
                .build();

        StorageImage storageImage = StorageImage.builder()
                .objectName(OBJECT_NAME)
                .bucketName("test-bucket")
                .build();

        ImageAnalysisMetadata visionMetadata = ImageAnalysisMetadata.builder()
                .imageLabels(List.of())
                .build();

        Embedding embedding = Embedding.builder()
                .vector(List.of(0.1f, 0.2f))
                .dimensions(2)
                .build();

        HSBColor hsbColor = new HSBColor(0.5f, 0.5f, 0.5f);

        when(itemEntityService.save(eq(COLLECTION_KEY), any(ItemEntity.class))).thenReturn(savedItem);
        when(cloudStorageService.uploadImage(multipartFile, USER_ID, COLLECTION_KEY)).thenReturn(storageImage);
        when(visionApiService.analyzeImageFromFile(multipartFile)).thenReturn(visionMetadata);
        when(visionApiService.extractTags(visionMetadata)).thenReturn(List.of("cap", "metal"));
        when(embeddingService.isAvailable()).thenReturn(true);
        when(embeddingService.generateEmbedding(multipartFile)).thenReturn(embedding);
        when(searchTokenService.generateTokens(any(ItemEntity.class))).thenReturn(List.of("new", "cap"));
        when(cloudStorageService.generateSignedUrl(anyString())).thenReturn(SIGNED_URL);

        try (MockedStatic<com.km.bottlecapcollector.color.HSBColorService> mockedHSBColorService =
                     mockStatic(com.km.bottlecapcollector.color.HSBColorService.class)) {
            mockedHSBColorService.when(() -> com.km.bottlecapcollector.color.HSBColorService.calculateColor(multipartFile))
                    .thenReturn(hsbColor);

            // When
            CollectionItemResponse result = collectionService.createCollectionItem(COLLECTION_KEY, USER_ID, request, multipartFile);

            // Then
            assertNotNull(result);
            verify(cloudStorageService).uploadImage(multipartFile, USER_ID, COLLECTION_KEY);
            verify(visionApiService).analyzeImageFromFile(multipartFile);
            verify(embeddingService).generateEmbedding(multipartFile);
            verify(searchTokenService).generateTokens(any(ItemEntity.class));
            verify(userService).addCollectionToUser(USER_ID, COLLECTION_KEY, COLLECTION_NAME);
        }
    }

    @Test
    void createCollectionItem_shouldSkipEmbeddingWhenServiceNotAvailable() throws IOException {
        // Given
        CreateCollectionItemRequest request = CreateCollectionItemRequest.builder()
                .name("New Cap")
                .collectionName(COLLECTION_NAME)
                .build();

        ItemEntity savedItem = ItemEntity.builder()
                .id(ITEM_ID)
                .name("New Cap")
                .userId(USER_ID)
                .collectionKey(COLLECTION_KEY)
                .tags(new ArrayList<>())
                .build();

        StorageImage storageImage = StorageImage.builder()
                .objectName(OBJECT_NAME)
                .bucketName("test-bucket")
                .build();

        ImageAnalysisMetadata visionMetadata = ImageAnalysisMetadata.builder()
                .imageLabels(List.of())
                .build();

        HSBColor hsbColor = new HSBColor(0.5f, 0.5f, 0.5f);

        when(itemEntityService.save(eq(COLLECTION_KEY), any(ItemEntity.class))).thenReturn(savedItem);
        when(cloudStorageService.uploadImage(multipartFile, USER_ID, COLLECTION_KEY)).thenReturn(storageImage);
        when(visionApiService.analyzeImageFromFile(multipartFile)).thenReturn(visionMetadata);
        when(visionApiService.extractTags(visionMetadata)).thenReturn(List.of());
        when(embeddingService.isAvailable()).thenReturn(false);
        when(searchTokenService.generateTokens(any(ItemEntity.class))).thenReturn(List.of());
        when(cloudStorageService.generateSignedUrl(anyString())).thenReturn(SIGNED_URL);

        try (MockedStatic<com.km.bottlecapcollector.color.HSBColorService> mockedHSBColorService =
                     mockStatic(com.km.bottlecapcollector.color.HSBColorService.class)) {
            mockedHSBColorService.when(() -> com.km.bottlecapcollector.color.HSBColorService.calculateColor(multipartFile))
                    .thenReturn(hsbColor);

            // When
            collectionService.createCollectionItem(COLLECTION_KEY, USER_ID, request, multipartFile);

            // Then
            verify(embeddingService, never()).generateEmbedding(any());
        }
    }

    @Test
    void createCollectionItem_shouldCleanupOnFailure() throws IOException {
        // Given
        CreateCollectionItemRequest request = CreateCollectionItemRequest.builder()
                .name("New Cap")
                .collectionName(COLLECTION_NAME)
                .build();

        StorageImageEntity imageEntity = StorageImageEntity.builder()
                .objectName(OBJECT_NAME)
                .build();

        ItemEntity savedItem = ItemEntity.builder()
                .id(ITEM_ID)
                .name("New Cap")
                .userId(USER_ID)
                .collectionKey(COLLECTION_KEY)
                .tags(new ArrayList<>())
                .image(imageEntity)
                .build();

        StorageImage storageImage = StorageImage.builder()
                .objectName(OBJECT_NAME)
                .bucketName("test-bucket")
                .build();

        HSBColor hsbColor = new HSBColor(0.5f, 0.5f, 0.5f);

        when(itemEntityService.save(eq(COLLECTION_KEY), any(ItemEntity.class))).thenReturn(savedItem);
        when(cloudStorageService.uploadImage(multipartFile, USER_ID, COLLECTION_KEY)).thenReturn(storageImage);
        when(visionApiService.analyzeImageFromFile(multipartFile)).thenThrow(new RuntimeException("Vision API error"));

        try (MockedStatic<com.km.bottlecapcollector.color.HSBColorService> mockedHSBColorService =
                     mockStatic(com.km.bottlecapcollector.color.HSBColorService.class)) {
            mockedHSBColorService.when(() -> com.km.bottlecapcollector.color.HSBColorService.calculateColor(multipartFile))
                    .thenReturn(hsbColor);

            // When / Then
            assertThrows(RuntimeException.class, () -> {
                collectionService.createCollectionItem(COLLECTION_KEY, USER_ID, request, multipartFile);
            });

            verify(cloudStorageService).deleteImage(OBJECT_NAME);
            verify(itemEntityService).deleteById(COLLECTION_KEY, ITEM_ID);
        }
    }

    @Test
    void updateImage_shouldDeleteOldImageAndProcessNewOne() throws IOException {
        // Given
        String oldObjectName = "item-456/old-image.jpg";
        StorageImageEntity oldImageEntity = StorageImageEntity.builder()
                .objectName(oldObjectName)
                .build();
        ItemEntity existingItem = ItemEntity.builder()
                .id(ITEM_ID)
                .name("Test Cap")
                .userId(USER_ID)
                .collectionKey(COLLECTION_KEY)
                .image(oldImageEntity)
                .tags(new ArrayList<>())
                .build();

        StorageImage newStorageImage = StorageImage.builder()
                .objectName(OBJECT_NAME)
                .bucketName("test-bucket")
                .build();

        ImageAnalysisMetadata visionMetadata = ImageAnalysisMetadata.builder()
                .imageLabels(List.of())
                .build();

        HSBColor hsbColor = new HSBColor(0.5f, 0.5f, 0.5f);

        when(itemEntityService.findByIdAndUserIdOrThrow(COLLECTION_KEY, ITEM_ID, USER_ID)).thenReturn(existingItem);
        when(cloudStorageService.uploadImage(multipartFile, USER_ID, COLLECTION_KEY)).thenReturn(newStorageImage);
        when(visionApiService.analyzeImageFromFile(multipartFile)).thenReturn(visionMetadata);
        when(visionApiService.extractTags(visionMetadata)).thenReturn(List.of());
        when(embeddingService.isAvailable()).thenReturn(false);
        when(searchTokenService.generateTokens(any(ItemEntity.class))).thenReturn(List.of());
        when(itemEntityService.save(eq(COLLECTION_KEY), any(ItemEntity.class))).thenAnswer(inv -> inv.getArgument(1));
        when(userService.getCollectionName(USER_ID, COLLECTION_KEY)).thenReturn(COLLECTION_NAME);
        when(cloudStorageService.generateSignedUrl(anyString())).thenReturn(SIGNED_URL);

        try (MockedStatic<com.km.bottlecapcollector.color.HSBColorService> mockedHSBColorService =
                     mockStatic(com.km.bottlecapcollector.color.HSBColorService.class)) {
            mockedHSBColorService.when(() -> com.km.bottlecapcollector.color.HSBColorService.calculateColor(multipartFile))
                    .thenReturn(hsbColor);

            // When
            CollectionItemResponse result = collectionService.updateImage(COLLECTION_KEY, ITEM_ID, USER_ID, multipartFile);

            // Then
            assertNotNull(result);
            verify(cloudStorageService).deleteImage(oldObjectName);
            verify(cloudStorageService).uploadImage(multipartFile, USER_ID, COLLECTION_KEY);
        }
    }

    @Test
    void updateImage_shouldHandleItemWithoutPreviousImage() throws IOException {
        // Given
        ItemEntity existingItem = ItemEntity.builder()
                .id(ITEM_ID)
                .name("Test Cap")
                .userId(USER_ID)
                .collectionKey(COLLECTION_KEY)
                .image(null)
                .tags(new ArrayList<>())
                .build();

        StorageImage newStorageImage = StorageImage.builder()
                .objectName(OBJECT_NAME)
                .bucketName("test-bucket")
                .build();

        ImageAnalysisMetadata visionMetadata = ImageAnalysisMetadata.builder()
                .imageLabels(List.of())
                .build();

        HSBColor hsbColor = new HSBColor(0.5f, 0.5f, 0.5f);

        when(itemEntityService.findByIdAndUserIdOrThrow(COLLECTION_KEY, ITEM_ID, USER_ID)).thenReturn(existingItem);
        when(cloudStorageService.uploadImage(multipartFile, USER_ID, COLLECTION_KEY)).thenReturn(newStorageImage);
        when(visionApiService.analyzeImageFromFile(multipartFile)).thenReturn(visionMetadata);
        when(visionApiService.extractTags(visionMetadata)).thenReturn(List.of());
        when(embeddingService.isAvailable()).thenReturn(false);
        when(searchTokenService.generateTokens(any(ItemEntity.class))).thenReturn(List.of());
        when(itemEntityService.save(eq(COLLECTION_KEY), any(ItemEntity.class))).thenAnswer(inv -> inv.getArgument(1));
        when(userService.getCollectionName(USER_ID, COLLECTION_KEY)).thenReturn(COLLECTION_NAME);
        when(cloudStorageService.generateSignedUrl(anyString())).thenReturn(SIGNED_URL);

        try (MockedStatic<com.km.bottlecapcollector.color.HSBColorService> mockedHSBColorService =
                     mockStatic(com.km.bottlecapcollector.color.HSBColorService.class)) {
            mockedHSBColorService.when(() -> com.km.bottlecapcollector.color.HSBColorService.calculateColor(multipartFile))
                    .thenReturn(hsbColor);

            // When
            collectionService.updateImage(COLLECTION_KEY, ITEM_ID, USER_ID, multipartFile);

            // Then
            verify(cloudStorageService, never()).deleteImage(anyString());
            verify(cloudStorageService).uploadImage(multipartFile, USER_ID, COLLECTION_KEY);
        }
    }

    private ItemEntity createItemEntity(String id, String name) {
        StorageImageEntity imageEntity = StorageImageEntity.builder()
                .objectName(id + "/image.jpg")
                .build();
        return ItemEntity.builder()
                .id(id)
                .name(name)
                .userId(USER_ID)
                .collectionKey(COLLECTION_KEY)
                .image(imageEntity)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
