package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.api.handler.exception.AppForbiddenException;
import com.km.bottlecapcollector.api.handler.exception.AppResourceNotFoundException;
import com.km.bottlecapcollector.api.model.response.CollectionShareResponse;
import com.km.bottlecapcollector.api.model.response.ShareCollectionResponse;
import com.km.bottlecapcollector.api.model.response.SharedCollectionResponse;
import com.km.bottlecapcollector.cloud.database.user.entity.CollectionShareEntity;
import com.km.bottlecapcollector.cloud.database.user.entity.SharedCollectionEntity;
import com.km.bottlecapcollector.cloud.database.user.entity.UserCollectionEntity;
import com.km.bottlecapcollector.cloud.database.user.entity.UserEntity;
import com.km.bottlecapcollector.cloud.database.user.repository.UserEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionShareServiceTests {

    private static final String OWNER_USER_ID = "owner-123";
    private static final String TARGET_USER_ID = "target-456";
    private static final String TARGET_EMAIL = "target@example.com";
    private static final String COLLECTION_KEY = "collection-key-uuid";
    private static final String COLLECTION_NAME = "My Bottle Caps";
    private static final String OWNER_NAME = "Owner Name";

    @Mock
    private UserEntityRepository userRepository;

    private CollectionShareService collectionShareService;

    @BeforeEach
    void setUp() {
        collectionShareService = new CollectionShareService(userRepository);
    }

    @Nested
    class ShareCollectionTests {

        @Test
        void shouldShareCollectionSuccessfully() {
            // Given
            UserEntity owner = createOwnerWithCollection();
            UserEntity targetUser = UserEntity.builder()
                    .id(TARGET_USER_ID)
                    .email(TARGET_EMAIL)
                    .name("Target User")
                    .sharedWithMe(new ArrayList<>())
                    .build();

            when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.of(owner));
            when(userRepository.findByEmail(TARGET_EMAIL)).thenReturn(Optional.of(targetUser));

            // When
            ShareCollectionResponse result = collectionShareService.shareCollection(
                    OWNER_USER_ID, COLLECTION_KEY, TARGET_EMAIL);

            // Then
            assertTrue(result.isShared());
            assertEquals("Collection shared successfully", result.getMessage());

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository, times(2)).save(captor.capture());

            List<UserEntity> savedUsers = captor.getAllValues();

            // Verify owner's sharesGranted was updated
            UserEntity savedOwner = savedUsers.get(0);
            assertEquals(1, savedOwner.getSharesGranted().size());
            CollectionShareEntity share = savedOwner.getSharesGranted().get(0);
            assertEquals(COLLECTION_KEY, share.getCollectionKey());
            assertEquals(TARGET_USER_ID, share.getSharedWithUserId());
            assertEquals(TARGET_EMAIL, share.getSharedWithEmail());

            // Verify target's sharedWithMe was updated
            UserEntity savedTarget = savedUsers.get(1);
            assertEquals(1, savedTarget.getSharedWithMe().size());
            SharedCollectionEntity sharedCollection = savedTarget.getSharedWithMe().get(0);
            assertEquals(COLLECTION_KEY, sharedCollection.getCollectionKey());
            assertEquals(COLLECTION_NAME, sharedCollection.getCollectionName());
            assertEquals(OWNER_USER_ID, sharedCollection.getOwnerUserId());
        }

        @Test
        void shouldReturnFalseWhenTargetUserNotFound() {
            // Given
            UserEntity owner = createOwnerWithCollection();
            when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.of(owner));
            when(userRepository.findByEmail(TARGET_EMAIL)).thenReturn(Optional.empty());

            // When
            ShareCollectionResponse result = collectionShareService.shareCollection(
                    OWNER_USER_ID, COLLECTION_KEY, TARGET_EMAIL);

            // Then
            assertFalse(result.isShared());
            assertEquals("User not found", result.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldReturnFalseWhenSharingWithYourself() {
            // Given
            UserEntity owner = createOwnerWithCollection();
            owner.setEmail(TARGET_EMAIL);

            when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.of(owner));
            when(userRepository.findByEmail(TARGET_EMAIL)).thenReturn(Optional.of(owner));

            // When
            ShareCollectionResponse result = collectionShareService.shareCollection(
                    OWNER_USER_ID, COLLECTION_KEY, TARGET_EMAIL);

            // Then
            assertFalse(result.isShared());
            assertEquals("Cannot share collection with yourself", result.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldReturnFalseWhenAlreadyShared() {
            // Given
            UserEntity owner = createOwnerWithCollection();
            owner.setSharesGranted(new ArrayList<>(List.of(
                    CollectionShareEntity.builder()
                            .collectionKey(COLLECTION_KEY)
                            .sharedWithUserId(TARGET_USER_ID)
                            .sharedWithEmail(TARGET_EMAIL)
                            .sharedAt(Instant.now())
                            .build()
            )));

            UserEntity targetUser = UserEntity.builder()
                    .id(TARGET_USER_ID)
                    .email(TARGET_EMAIL)
                    .build();

            when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.of(owner));
            when(userRepository.findByEmail(TARGET_EMAIL)).thenReturn(Optional.of(targetUser));

            // When
            ShareCollectionResponse result = collectionShareService.shareCollection(
                    OWNER_USER_ID, COLLECTION_KEY, TARGET_EMAIL);

            // Then
            assertFalse(result.isShared());
            assertEquals("Collection already shared with this user", result.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenOwnerNotFound() {
            // Given
            when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThrows(AppResourceNotFoundException.class, () ->
                    collectionShareService.shareCollection(OWNER_USER_ID, COLLECTION_KEY, TARGET_EMAIL));
        }

        @Test
        void shouldThrowExceptionWhenOwnerDoesNotOwnCollection() {
            // Given
            UserEntity owner = UserEntity.builder()
                    .id(OWNER_USER_ID)
                    .collections(new ArrayList<>())
                    .build();

            when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.of(owner));

            // When / Then
            assertThrows(AppForbiddenException.class, () ->
                    collectionShareService.shareCollection(OWNER_USER_ID, COLLECTION_KEY, TARGET_EMAIL));
        }
    }

    @Nested
    class RevokeShareTests {

        @Test
        void shouldRevokeShareSuccessfully() {
            // Given
            UserEntity owner = createOwnerWithCollection();
            owner.setSharesGranted(new ArrayList<>(List.of(
                    CollectionShareEntity.builder()
                            .collectionKey(COLLECTION_KEY)
                            .sharedWithUserId(TARGET_USER_ID)
                            .sharedWithEmail(TARGET_EMAIL)
                            .sharedAt(Instant.now())
                            .build()
            )));

            UserEntity targetUser = UserEntity.builder()
                    .id(TARGET_USER_ID)
                    .sharedWithMe(new ArrayList<>(List.of(
                            SharedCollectionEntity.builder()
                                    .collectionKey(COLLECTION_KEY)
                                    .ownerUserId(OWNER_USER_ID)
                                    .build()
                    )))
                    .build();

            when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.of(owner));
            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));

            // When
            collectionShareService.revokeShare(OWNER_USER_ID, COLLECTION_KEY, TARGET_USER_ID);

            // Then
            verify(userRepository, times(2)).save(any(UserEntity.class));
            assertTrue(owner.getSharesGranted().isEmpty());
            assertTrue(targetUser.getSharedWithMe().isEmpty());
        }

        @Test
        void shouldHandleTargetUserNotFound() {
            // Given
            UserEntity owner = createOwnerWithCollection();
            owner.setSharesGranted(new ArrayList<>(List.of(
                    CollectionShareEntity.builder()
                            .collectionKey(COLLECTION_KEY)
                            .sharedWithUserId(TARGET_USER_ID)
                            .build()
            )));

            when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.of(owner));
            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.empty());

            // When - should not throw
            collectionShareService.revokeShare(OWNER_USER_ID, COLLECTION_KEY, TARGET_USER_ID);

            // Then - owner's share should still be removed
            verify(userRepository).save(owner);
            assertTrue(owner.getSharesGranted().isEmpty());
        }

        @Test
        void shouldThrowExceptionWhenOwnerDoesNotOwnCollection() {
            // Given
            UserEntity owner = UserEntity.builder()
                    .id(OWNER_USER_ID)
                    .collections(new ArrayList<>())
                    .build();

            when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.of(owner));

            // When / Then
            assertThrows(AppForbiddenException.class, () ->
                    collectionShareService.revokeShare(OWNER_USER_ID, COLLECTION_KEY, TARGET_USER_ID));
        }
    }

    @Nested
    class GetSharesForCollectionTests {

        @Test
        void shouldReturnSharesForCollection() {
            // Given
            Instant sharedAt = Instant.now();
            UserEntity owner = createOwnerWithCollection();
            owner.setSharesGranted(new ArrayList<>(List.of(
                    CollectionShareEntity.builder()
                            .collectionKey(COLLECTION_KEY)
                            .sharedWithUserId(TARGET_USER_ID)
                            .sharedWithEmail(TARGET_EMAIL)
                            .sharedAt(sharedAt)
                            .build(),
                    CollectionShareEntity.builder()
                            .collectionKey("other-collection")
                            .sharedWithUserId("other-user")
                            .sharedWithEmail("other@example.com")
                            .sharedAt(sharedAt)
                            .build()
            )));

            when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.of(owner));

            // When
            List<CollectionShareResponse> result = collectionShareService.getSharesForCollection(
                    OWNER_USER_ID, COLLECTION_KEY);

            // Then
            assertEquals(1, result.size());
            assertEquals(TARGET_USER_ID, result.get(0).getUserId());
            assertEquals(TARGET_EMAIL, result.get(0).getEmail());
            assertEquals(sharedAt, result.get(0).getSharedAt());
        }

        @Test
        void shouldReturnEmptyListWhenNoShares() {
            // Given
            UserEntity owner = createOwnerWithCollection();
            owner.setSharesGranted(null);

            when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.of(owner));

            // When
            List<CollectionShareResponse> result = collectionShareService.getSharesForCollection(
                    OWNER_USER_ID, COLLECTION_KEY);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class GetCollectionsSharedWithUserTests {

        @Test
        void shouldReturnSharedCollections() {
            // Given
            Instant sharedAt = Instant.now();
            UserEntity user = UserEntity.builder()
                    .id(TARGET_USER_ID)
                    .sharedWithMe(new ArrayList<>(List.of(
                            SharedCollectionEntity.builder()
                                    .collectionKey(COLLECTION_KEY)
                                    .collectionName(COLLECTION_NAME)
                                    .ownerUserId(OWNER_USER_ID)
                                    .ownerName(OWNER_NAME)
                                    .sharedAt(sharedAt)
                                    .build()
                    )))
                    .build();

            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));

            // When
            List<SharedCollectionResponse> result = collectionShareService.getCollectionsSharedWithUser(TARGET_USER_ID);

            // Then
            assertEquals(1, result.size());
            assertEquals(COLLECTION_KEY, result.get(0).getCollectionKey());
            assertEquals(COLLECTION_NAME, result.get(0).getCollectionName());
            assertEquals(OWNER_USER_ID, result.get(0).getOwnerUserId());
            assertEquals(OWNER_NAME, result.get(0).getOwnerName());
            assertEquals(sharedAt, result.get(0).getSharedAt());
        }

        @Test
        void shouldReturnEmptyListWhenNoSharedCollections() {
            // Given
            UserEntity user = UserEntity.builder()
                    .id(TARGET_USER_ID)
                    .sharedWithMe(null)
                    .build();

            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));

            // When
            List<SharedCollectionResponse> result = collectionShareService.getCollectionsSharedWithUser(TARGET_USER_ID);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class ResolveCollectionOwnerTests {

        @Test
        void shouldReturnUserIdWhenUserOwnsCollection() {
            // Given
            UserEntity user = createOwnerWithCollection();
            user.setId(TARGET_USER_ID);

            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));

            // When
            String result = collectionShareService.resolveCollectionOwner(TARGET_USER_ID, COLLECTION_KEY);

            // Then
            assertEquals(TARGET_USER_ID, result);
        }

        @Test
        void shouldReturnOwnerIdWhenCollectionIsSharedWithUser() {
            // Given
            UserEntity user = UserEntity.builder()
                    .id(TARGET_USER_ID)
                    .collections(new ArrayList<>())
                    .sharedWithMe(new ArrayList<>(List.of(
                            SharedCollectionEntity.builder()
                                    .collectionKey(COLLECTION_KEY)
                                    .ownerUserId(OWNER_USER_ID)
                                    .build()
                    )))
                    .build();

            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));

            // When
            String result = collectionShareService.resolveCollectionOwner(TARGET_USER_ID, COLLECTION_KEY);

            // Then
            assertEquals(OWNER_USER_ID, result);
        }

        @Test
        void shouldThrowExceptionWhenNoAccess() {
            // Given
            UserEntity user = UserEntity.builder()
                    .id(TARGET_USER_ID)
                    .collections(new ArrayList<>())
                    .sharedWithMe(new ArrayList<>())
                    .build();

            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));

            // When / Then
            AppForbiddenException exception = assertThrows(AppForbiddenException.class, () ->
                    collectionShareService.resolveCollectionOwner(TARGET_USER_ID, COLLECTION_KEY));
            assertEquals("You do not have access to this collection", exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThrows(AppResourceNotFoundException.class, () ->
                    collectionShareService.resolveCollectionOwner(TARGET_USER_ID, COLLECTION_KEY));
        }
    }

    @Nested
    class HasReadAccessTests {

        @Test
        void shouldReturnTrueWhenUserIsOwner() {
            // When
            boolean result = collectionShareService.hasReadAccess(OWNER_USER_ID, COLLECTION_KEY, OWNER_USER_ID);

            // Then
            assertTrue(result);
        }

        @Test
        void shouldReturnTrueWhenCollectionIsSharedWithUser() {
            // Given
            UserEntity user = UserEntity.builder()
                    .id(TARGET_USER_ID)
                    .sharedWithMe(new ArrayList<>(List.of(
                            SharedCollectionEntity.builder()
                                    .collectionKey(COLLECTION_KEY)
                                    .ownerUserId(OWNER_USER_ID)
                                    .build()
                    )))
                    .build();

            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));

            // When
            boolean result = collectionShareService.hasReadAccess(TARGET_USER_ID, COLLECTION_KEY, OWNER_USER_ID);

            // Then
            assertTrue(result);
        }

        @Test
        void shouldReturnFalseWhenCollectionNotSharedWithUser() {
            // Given
            UserEntity user = UserEntity.builder()
                    .id(TARGET_USER_ID)
                    .sharedWithMe(new ArrayList<>())
                    .build();

            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));

            // When
            boolean result = collectionShareService.hasReadAccess(TARGET_USER_ID, COLLECTION_KEY, OWNER_USER_ID);

            // Then
            assertFalse(result);
        }

        @Test
        void shouldReturnFalseWhenUserNotFound() {
            // Given
            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.empty());

            // When
            boolean result = collectionShareService.hasReadAccess(TARGET_USER_ID, COLLECTION_KEY, OWNER_USER_ID);

            // Then
            assertFalse(result);
        }

        @Test
        void shouldReturnFalseWhenSharedWithMeIsNull() {
            // Given
            UserEntity user = UserEntity.builder()
                    .id(TARGET_USER_ID)
                    .sharedWithMe(null)
                    .build();

            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));

            // When
            boolean result = collectionShareService.hasReadAccess(TARGET_USER_ID, COLLECTION_KEY, OWNER_USER_ID);

            // Then
            assertFalse(result);
        }
    }

    private UserEntity createOwnerWithCollection() {
        List<UserCollectionEntity> collections = new ArrayList<>();
        collections.add(UserCollectionEntity.builder()
                .collectionKey(COLLECTION_KEY)
                .collectionName(COLLECTION_NAME)
                .build());

        return UserEntity.builder()
                .id(OWNER_USER_ID)
                .name(OWNER_NAME)
                .email("owner@example.com")
                .collections(collections)
                .sharesGranted(new ArrayList<>())
                .createdAt(Instant.now())
                .build();
    }
}
