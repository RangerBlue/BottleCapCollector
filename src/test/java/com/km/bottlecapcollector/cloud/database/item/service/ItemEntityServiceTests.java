package com.km.bottlecapcollector.cloud.database.item.service;

import com.km.bottlecapcollector.cloud.database.item.entity.ItemEntity;
import com.km.bottlecapcollector.cloud.database.item.repository.CollectionItemEntityRepository;
import com.km.bottlecapcollector.cloud.database.exception.FirestoreDocumentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemEntityServiceTests {

    private static final String COLLECTION_KEY = "test-collection";
    private static final String ITEM_ID = "item-123";
    private static final String USER_ID = "user-456";

    @Mock
    private CollectionItemEntityRepository repository;

    private ItemEntityService itemEntityService;

    @BeforeEach
    void setUp() {
        itemEntityService = new ItemEntityService(repository);
    }

    @Test
    void save_shouldDelegateToRepository() {
        // Given
        ItemEntity item = createItemEntity(ITEM_ID, "Test Item");
        ItemEntity savedItem = createItemEntity(ITEM_ID, "Test Item");

        when(repository.save(COLLECTION_KEY, item)).thenReturn(savedItem);

        // When
        ItemEntity result = itemEntityService.save(COLLECTION_KEY, item);

        // Then
        assertNotNull(result);
        assertEquals(ITEM_ID, result.getId());
        verify(repository).save(COLLECTION_KEY, item);
    }

    @Test
    void findById_shouldReturnItemWhenFound() {
        // Given
        ItemEntity item = createItemEntity(ITEM_ID, "Test Item");
        when(repository.findById(COLLECTION_KEY, ITEM_ID)).thenReturn(Optional.of(item));

        // When
        Optional<ItemEntity> result = itemEntityService.findById(COLLECTION_KEY, ITEM_ID);

        // Then
        assertTrue(result.isPresent());
        assertEquals(ITEM_ID, result.get().getId());
        verify(repository).findById(COLLECTION_KEY, ITEM_ID);
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        // Given
        when(repository.findById(COLLECTION_KEY, ITEM_ID)).thenReturn(Optional.empty());

        // When
        Optional<ItemEntity> result = itemEntityService.findById(COLLECTION_KEY, ITEM_ID);

        // Then
        assertFalse(result.isPresent());
        verify(repository).findById(COLLECTION_KEY, ITEM_ID);
    }

    @Test
    void findByIdOrThrow_shouldReturnItemWhenFound() {
        // Given
        ItemEntity item = createItemEntity(ITEM_ID, "Test Item");
        when(repository.findById(COLLECTION_KEY, ITEM_ID)).thenReturn(Optional.of(item));

        // When
        ItemEntity result = itemEntityService.findByIdOrThrow(COLLECTION_KEY, ITEM_ID);

        // Then
        assertNotNull(result);
        assertEquals(ITEM_ID, result.getId());
    }

    @Test
    void findByIdOrThrow_shouldThrowExceptionWhenNotFound() {
        // Given
        when(repository.findById(COLLECTION_KEY, ITEM_ID)).thenReturn(Optional.empty());

        // When / Then
        FirestoreDocumentNotFoundException exception = assertThrows(
                FirestoreDocumentNotFoundException.class,
                () -> itemEntityService.findByIdOrThrow(COLLECTION_KEY, ITEM_ID)
        );

        assertNotNull(exception);
    }

    @Test
    void findByIdAndUserId_shouldReturnItemWhenFound() {
        // Given
        ItemEntity item = createItemEntity(ITEM_ID, "Test Item");
        when(repository.findByIdAndUserId(COLLECTION_KEY, ITEM_ID, USER_ID)).thenReturn(Optional.of(item));

        // When
        Optional<ItemEntity> result = itemEntityService.findByIdAndUserId(COLLECTION_KEY, ITEM_ID, USER_ID);

        // Then
        assertTrue(result.isPresent());
        assertEquals(ITEM_ID, result.get().getId());
        verify(repository).findByIdAndUserId(COLLECTION_KEY, ITEM_ID, USER_ID);
    }

    @Test
    void findByIdAndUserId_shouldReturnEmptyWhenNotFound() {
        // Given
        when(repository.findByIdAndUserId(COLLECTION_KEY, ITEM_ID, USER_ID)).thenReturn(Optional.empty());

        // When
        Optional<ItemEntity> result = itemEntityService.findByIdAndUserId(COLLECTION_KEY, ITEM_ID, USER_ID);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void findByIdAndUserIdOrThrow_shouldReturnItemWhenFound() {
        // Given
        ItemEntity item = createItemEntity(ITEM_ID, "Test Item");
        when(repository.findByIdAndUserId(COLLECTION_KEY, ITEM_ID, USER_ID)).thenReturn(Optional.of(item));

        // When
        ItemEntity result = itemEntityService.findByIdAndUserIdOrThrow(COLLECTION_KEY, ITEM_ID, USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(ITEM_ID, result.getId());
    }

    @Test
    void findByIdAndUserIdOrThrow_shouldThrowExceptionWhenNotFound() {
        // Given
        when(repository.findByIdAndUserId(COLLECTION_KEY, ITEM_ID, USER_ID)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(
                FirestoreDocumentNotFoundException.class,
                () -> itemEntityService.findByIdAndUserIdOrThrow(COLLECTION_KEY, ITEM_ID, USER_ID)
        );
    }

    @Test
    void findByUserId_shouldReturnListOfItems() {
        // Given
        int limit = 10;
        int offset = 0;
        List<ItemEntity> items = List.of(
                createItemEntity("item-1", "Item 1"),
                createItemEntity("item-2", "Item 2")
        );
        when(repository.findByUserId(COLLECTION_KEY, USER_ID, limit, offset)).thenReturn(items);

        // When
        List<ItemEntity> result = itemEntityService.findByUserId(COLLECTION_KEY, USER_ID, limit, offset);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByUserId(COLLECTION_KEY, USER_ID, limit, offset);
    }

    @Test
    void findByUserId_shouldReturnEmptyListWhenNoItems() {
        // Given
        when(repository.findByUserId(COLLECTION_KEY, USER_ID, 10, 0)).thenReturn(List.of());

        // When
        List<ItemEntity> result = itemEntityService.findByUserId(COLLECTION_KEY, USER_ID, 10, 0);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void countByUserId_shouldReturnCount() {
        // Given
        when(repository.countByUserId(COLLECTION_KEY, USER_ID)).thenReturn(5L);

        // When
        long result = itemEntityService.countByUserId(COLLECTION_KEY, USER_ID);

        // Then
        assertEquals(5L, result);
        verify(repository).countByUserId(COLLECTION_KEY, USER_ID);
    }

    @Test
    void countByUserId_shouldReturnZeroWhenNoItems() {
        // Given
        when(repository.countByUserId(COLLECTION_KEY, USER_ID)).thenReturn(0L);

        // When
        long result = itemEntityService.countByUserId(COLLECTION_KEY, USER_ID);

        // Then
        assertEquals(0L, result);
    }

    @Test
    void findBySearchTokenAndUserId_shouldReturnMatchingItems() {
        // Given
        String searchToken = "beer";
        int limit = 10;
        int offset = 0;
        List<ItemEntity> items = List.of(createItemEntity("item-1", "Beer Cap"));

        when(repository.findBySearchTokenAndUserId(COLLECTION_KEY, searchToken, USER_ID, limit, offset))
                .thenReturn(items);

        // When
        List<ItemEntity> result = itemEntityService.findBySearchTokenAndUserId(
                COLLECTION_KEY, searchToken, USER_ID, limit, offset);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findBySearchTokenAndUserId(COLLECTION_KEY, searchToken, USER_ID, limit, offset);
    }

    @Test
    void countBySearchTokenAndUserId_shouldReturnCount() {
        // Given
        String searchToken = "beer";
        when(repository.countBySearchTokenAndUserId(COLLECTION_KEY, searchToken, USER_ID)).thenReturn(3L);

        // When
        long result = itemEntityService.countBySearchTokenAndUserId(COLLECTION_KEY, searchToken, USER_ID);

        // Then
        assertEquals(3L, result);
        verify(repository).countBySearchTokenAndUserId(COLLECTION_KEY, searchToken, USER_ID);
    }

    @Test
    void findByHSBColorRangeAndUserId_shouldReturnMatchingItems() {
        // Given
        float hueMin = 0.0f, hueMax = 0.2f;
        float satMin = 0.5f, satMax = 1.0f;
        float briMin = 0.3f, briMax = 0.8f;
        int limit = 30;

        List<ItemEntity> items = List.of(
                createItemEntity("item-1", "Red Cap"),
                createItemEntity("item-2", "Orange Cap")
        );

        when(repository.findByHSBColorRangeAndUserId(
                COLLECTION_KEY, hueMin, hueMax, satMin, satMax, briMin, briMax, USER_ID, limit))
                .thenReturn(items);

        // When
        List<ItemEntity> result = itemEntityService.findByHSBColorRangeAndUserId(
                COLLECTION_KEY, hueMin, hueMax, satMin, satMax, briMin, briMax, USER_ID, limit);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByHSBColorRangeAndUserId(
                COLLECTION_KEY, hueMin, hueMax, satMin, satMax, briMin, briMax, USER_ID, limit);
    }

    @Test
    void deleteById_shouldDelegateToRepository() {
        // When
        itemEntityService.deleteById(COLLECTION_KEY, ITEM_ID);

        // Then
        verify(repository).deleteById(COLLECTION_KEY, ITEM_ID);
    }

    @Test
    void deleteByIdAndUserId_shouldDeleteWhenItemExists() {
        // Given
        ItemEntity item = createItemEntity(ITEM_ID, "Test Item");
        when(repository.findByIdAndUserId(COLLECTION_KEY, ITEM_ID, USER_ID)).thenReturn(Optional.of(item));

        // When
        itemEntityService.deleteByIdAndUserId(COLLECTION_KEY, ITEM_ID, USER_ID);

        // Then
        verify(repository).findByIdAndUserId(COLLECTION_KEY, ITEM_ID, USER_ID);
        verify(repository).deleteById(COLLECTION_KEY, ITEM_ID);
    }

    @Test
    void deleteByIdAndUserId_shouldThrowExceptionWhenItemNotFound() {
        // Given
        when(repository.findByIdAndUserId(COLLECTION_KEY, ITEM_ID, USER_ID)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(
                FirestoreDocumentNotFoundException.class,
                () -> itemEntityService.deleteByIdAndUserId(COLLECTION_KEY, ITEM_ID, USER_ID)
        );

        verify(repository).findByIdAndUserId(COLLECTION_KEY, ITEM_ID, USER_ID);
    }

    private ItemEntity createItemEntity(String id, String name) {
        return ItemEntity.builder()
                .id(id)
                .name(name)
                .userId(USER_ID)
                .collectionKey(COLLECTION_KEY)
                .build();
    }
}
