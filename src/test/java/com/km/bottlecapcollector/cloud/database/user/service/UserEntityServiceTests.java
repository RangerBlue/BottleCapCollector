package com.km.bottlecapcollector.cloud.database.user.service;

import com.km.bottlecapcollector.cloud.database.user.entity.UserCollectionEntity;
import com.km.bottlecapcollector.cloud.database.user.entity.UserEntity;
import com.km.bottlecapcollector.cloud.database.user.repository.UserEntityRepository;
import com.km.bottlecapcollector.cloud.database.exception.FirestoreDocumentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserEntityServiceTests {

    private static final String USER_ID = "user-123";
    private static final String COLLECTION_KEY = "collection-key-uuid";
    private static final String COLLECTION_NAME = "My Bottle Caps";

    @Mock
    private UserEntityRepository userRepository;

    private UserEntityService userEntityService;

    @BeforeEach
    void setUp() {
        userEntityService = new UserEntityService(userRepository);
    }

    @Test
    void getCollections_shouldReturnCollectionsWhenUserExists() {
        // Given
        List<UserCollectionEntity> collections = List.of(
                UserCollectionEntity.builder()
                        .collectionKey(COLLECTION_KEY)
                        .collectionName(COLLECTION_NAME)
                        .build(),
                UserCollectionEntity.builder()
                        .collectionKey("key-2")
                        .collectionName("Second Collection")
                        .build()
        );
        UserEntity user = UserEntity.builder()
                .id(USER_ID)
                .collections(new ArrayList<>(collections))
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // When
        List<UserCollectionEntity> result = userEntityService.getCollections(USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(COLLECTION_KEY, result.get(0).getCollectionKey());
        assertEquals(COLLECTION_NAME, result.get(0).getCollectionName());
        verify(userRepository).findById(USER_ID);
    }

    @Test
    void getCollections_shouldReturnEmptyListWhenUserHasNoCollections() {
        // Given
        UserEntity user = UserEntity.builder()
                .id(USER_ID)
                .collections(null)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // When
        List<UserCollectionEntity> result = userEntityService.getCollections(USER_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getCollections_shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(
                FirestoreDocumentNotFoundException.class,
                () -> userEntityService.getCollections(USER_ID)
        );
    }

    @Test
    void getCollectionName_shouldReturnNameWhenFound() {
        // Given
        UserEntity user = createUserWithCollection(COLLECTION_KEY, COLLECTION_NAME);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // When
        String result = userEntityService.getCollectionName(USER_ID, COLLECTION_KEY);

        // Then
        assertEquals(COLLECTION_NAME, result);
    }

    @Test
    void getCollectionName_shouldReturnNullWhenUserNotFound() {
        // Given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When
        String result = userEntityService.getCollectionName(USER_ID, COLLECTION_KEY);

        // Then
        assertNull(result);
    }

    @Test
    void getCollectionName_shouldReturnNullWhenCollectionNotFound() {
        // Given
        UserEntity user = createUserWithCollection("different-key", "Different Collection");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // When
        String result = userEntityService.getCollectionName(USER_ID, COLLECTION_KEY);

        // Then
        assertNull(result);
    }

    @Test
    void addCollectionToUser_shouldAddCollectionToExistingUser() {
        // Given
        UserEntity existingUser = UserEntity.builder()
                .id(USER_ID)
                .collections(new ArrayList<>())
                .createdAt(Instant.now())
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        // When
        userEntityService.addCollectionToUser(USER_ID, COLLECTION_KEY, COLLECTION_NAME);

        // Then
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());

        UserEntity savedUser = captor.getValue();
        assertEquals(1, savedUser.getCollections().size());
        assertEquals(COLLECTION_KEY, savedUser.getCollections().get(0).getCollectionKey());
        assertEquals(COLLECTION_NAME, savedUser.getCollections().get(0).getCollectionName());
        assertNotNull(savedUser.getUpdatedAt());
    }

    @Test
    void addCollectionToUser_shouldCreateNewUserWhenNotExists() {
        // Given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When
        userEntityService.addCollectionToUser(USER_ID, COLLECTION_KEY, COLLECTION_NAME);

        // Then
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());

        UserEntity savedUser = captor.getValue();
        assertEquals(USER_ID, savedUser.getId());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
        assertEquals(1, savedUser.getCollections().size());
    }

    @Test
    void addCollectionToUser_shouldNotSaveWhenCollectionAlreadyExists() {
        // Given
        UserEntity existingUser = createUserWithCollection(COLLECTION_KEY, COLLECTION_NAME);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        // When
        userEntityService.addCollectionToUser(USER_ID, COLLECTION_KEY, COLLECTION_NAME);

        // Then
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void addCollectionToUser_shouldAddSecondCollection() {
        // Given
        UserEntity existingUser = createUserWithCollection("existing-key", "Existing Collection");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        // When
        userEntityService.addCollectionToUser(USER_ID, COLLECTION_KEY, COLLECTION_NAME);

        // Then
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());

        UserEntity savedUser = captor.getValue();
        assertEquals(2, savedUser.getCollections().size());
    }

    @Test
    void getUser_shouldReturnUserWhenFound() {
        // Given
        UserEntity user = UserEntity.builder()
                .id(USER_ID)
                .email("test@example.com")
                .name("Test User")
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // When
        UserEntity result = userEntityService.getUser(USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(USER_ID, result.getId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Test User", result.getName());
    }

    @Test
    void getUser_shouldThrowExceptionWhenNotFound() {
        // Given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(
                FirestoreDocumentNotFoundException.class,
                () -> userEntityService.getUser(USER_ID)
        );
    }

    @Test
    void userExists_shouldReturnTrueWhenUserExists() {
        // Given
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        // When
        boolean result = userEntityService.userExists(USER_ID);

        // Then
        assertTrue(result);
        verify(userRepository).existsById(USER_ID);
    }

    @Test
    void userExists_shouldReturnFalseWhenUserNotExists() {
        // Given
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        // When
        boolean result = userEntityService.userExists(USER_ID);

        // Then
        assertFalse(result);
    }

    private UserEntity createUserWithCollection(String collectionKey, String collectionName) {
        List<UserCollectionEntity> collections = new ArrayList<>();
        collections.add(UserCollectionEntity.builder()
                .collectionKey(collectionKey)
                .collectionName(collectionName)
                .build());

        return UserEntity.builder()
                .id(USER_ID)
                .collections(collections)
                .createdAt(Instant.now())
                .build();
    }
}
