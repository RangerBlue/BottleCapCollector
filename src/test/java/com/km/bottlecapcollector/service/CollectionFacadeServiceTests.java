package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.api.model.request.CreateCollectionItemRequest;
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
import com.km.bottlecapcollector.cloud.database.user.entity.UserCollectionEntity;
import com.km.bottlecapcollector.property.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionFacadeServiceTests {

    private static final String USER_ID = "user-123";
    private static final String COLLECTION_KEY = "collection-key-uuid";
    private static final String COLLECTION_NAME = "My Collection";
    private static final String ITEM_ID = "item-456";
    private static final String OWNER_USER_ID = "owner-789";

    @Mock
    private UserService userService;

    @Mock
    private CollectionService collectionService;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private CollectionShareService collectionShareService;

    @Mock
    private AppProperties appProperties;

    @Mock
    private OAuth2AuthenticatedPrincipal principal;

    @Mock
    private MultipartFile multipartFile;

    private CollectionFacadeService collectionFacadeService;

    @BeforeEach
    void setUp() {
        collectionFacadeService = new CollectionFacadeService(
                userService,
                collectionService,
                rateLimitService,
                collectionShareService,
                appProperties
        );
        when(userService.getUserId(principal)).thenReturn(USER_ID);
    }

    @Nested
    class GetUserCollectionsTests {

        @Test
        void getUserCollections_shouldReturnUserCollections() {
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
            when(userService.getCollections(USER_ID)).thenReturn(collections);

            // When
            List<UserCollectionResponse> result = collectionFacadeService.getUserCollections(principal);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(COLLECTION_KEY, result.get(0).getCollectionKey());
            assertEquals(COLLECTION_NAME, result.get(0).getCollectionName());
            assertEquals("key-2", result.get(1).getCollectionKey());
        }

        @Test
        void getUserCollections_shouldReturnEmptyListWhenNoCollections() {
            // Given
            when(userService.getCollections(USER_ID)).thenReturn(List.of());

            // When
            List<UserCollectionResponse> result = collectionFacadeService.getUserCollections(principal);

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    @Nested
    class CreateItemTests {

        @Test
        void createItem_shouldDelegateToCollectionService() {
            // Given
            CreateCollectionItemRequest request = CreateCollectionItemRequest.builder()
                    .name("New Cap")
                    .build();
            CollectionItemResponse expectedResponse = CollectionItemResponse.builder()
                    .id(ITEM_ID)
                    .name("New Cap")
                    .build();

            when(collectionService.createCollectionItem(COLLECTION_KEY, USER_ID, request, multipartFile))
                    .thenReturn(expectedResponse);

            // When
            CollectionItemResponse result = collectionFacadeService.createItem(
                    principal, COLLECTION_KEY, request, multipartFile);

            // Then
            assertEquals(expectedResponse, result);
            verify(collectionService).createCollectionItem(COLLECTION_KEY, USER_ID, request, multipartFile);
        }
    }

    @Nested
    class GetItemTests {

        @Test
        void getItem_shouldResolveOwnerAndDelegateToCollectionService() {
            // Given
            CollectionItemResponse expectedResponse = CollectionItemResponse.builder()
                    .id(ITEM_ID)
                    .name("Test Cap")
                    .build();

            when(collectionShareService.resolveCollectionOwner(USER_ID, COLLECTION_KEY))
                    .thenReturn(OWNER_USER_ID);
            when(collectionService.getCollectionItem(COLLECTION_KEY, ITEM_ID, OWNER_USER_ID))
                    .thenReturn(expectedResponse);

            // When
            CollectionItemResponse result = collectionFacadeService.getItem(
                    principal, COLLECTION_KEY, ITEM_ID);

            // Then
            assertEquals(expectedResponse, result);
            verify(collectionShareService).resolveCollectionOwner(USER_ID, COLLECTION_KEY);
            verify(collectionService).getCollectionItem(COLLECTION_KEY, ITEM_ID, OWNER_USER_ID);
        }

        @Test
        void getItem_shouldWorkWithOwnCollection() {
            // Given
            CollectionItemResponse expectedResponse = CollectionItemResponse.builder()
                    .id(ITEM_ID)
                    .name("Test Cap")
                    .build();

            when(collectionShareService.resolveCollectionOwner(USER_ID, COLLECTION_KEY))
                    .thenReturn(USER_ID);
            when(collectionService.getCollectionItem(COLLECTION_KEY, ITEM_ID, USER_ID))
                    .thenReturn(expectedResponse);

            // When
            CollectionItemResponse result = collectionFacadeService.getItem(
                    principal, COLLECTION_KEY, ITEM_ID);

            // Then
            assertEquals(expectedResponse, result);
            verify(collectionService).getCollectionItem(COLLECTION_KEY, ITEM_ID, USER_ID);
        }
    }

    @Nested
    class GetItemsTests {

        @Test
        void getItems_shouldResolveOwnerAndSearchItems() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            String query = "beer";
            List<CollectionItemSummary> items = List.of(
                    CollectionItemSummary.builder().id("item-1").name("Beer Cap").build()
            );
            Page<CollectionItemSummary> expectedPage = new PageImpl<>(items, pageable, 1);

            when(collectionShareService.resolveCollectionOwner(USER_ID, COLLECTION_KEY))
                    .thenReturn(OWNER_USER_ID);
            when(collectionService.searchItemsPaginated(COLLECTION_KEY, query, OWNER_USER_ID, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<CollectionItemSummary> result = collectionFacadeService.getItems(
                    principal, COLLECTION_KEY, query, pageable);

            // Then
            assertEquals(1, result.getTotalElements());
            verify(collectionService).searchItemsPaginated(COLLECTION_KEY, query, OWNER_USER_ID, pageable);
        }

        @Test
        void getItems_shouldHandleNullQuery() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<CollectionItemSummary> expectedPage = new PageImpl<>(List.of(), pageable, 0);

            when(collectionShareService.resolveCollectionOwner(USER_ID, COLLECTION_KEY))
                    .thenReturn(USER_ID);
            when(collectionService.searchItemsPaginated(COLLECTION_KEY, null, USER_ID, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<CollectionItemSummary> result = collectionFacadeService.getItems(
                    principal, COLLECTION_KEY, null, pageable);

            // Then
            assertEquals(0, result.getTotalElements());
        }
    }

    @Nested
    class UpdateItemTests {

        @Test
        void updateItem_shouldDelegateToCollectionService() {
            // Given
            UpdateCollectionItem request = UpdateCollectionItem.builder()
                    .name("Updated Name")
                    .build();
            CollectionItemResponse expectedResponse = CollectionItemResponse.builder()
                    .id(ITEM_ID)
                    .name("Updated Name")
                    .build();

            when(collectionService.updateItem(COLLECTION_KEY, ITEM_ID, USER_ID, request))
                    .thenReturn(expectedResponse);

            // When
            CollectionItemResponse result = collectionFacadeService.updateItem(
                    principal, COLLECTION_KEY, ITEM_ID, request);

            // Then
            assertEquals(expectedResponse, result);
            verify(collectionService).updateItem(COLLECTION_KEY, ITEM_ID, USER_ID, request);
        }
    }

    @Nested
    class UpdateImageTests {

        @Test
        void updateImage_shouldDelegateToCollectionService() {
            // Given
            CollectionItemResponse expectedResponse = CollectionItemResponse.builder()
                    .id(ITEM_ID)
                    .name("Test Cap")
                    .build();

            when(collectionService.updateImage(COLLECTION_KEY, ITEM_ID, USER_ID, multipartFile))
                    .thenReturn(expectedResponse);

            // When
            CollectionItemResponse result = collectionFacadeService.updateImage(
                    principal, COLLECTION_KEY, ITEM_ID, multipartFile);

            // Then
            assertEquals(expectedResponse, result);
            verify(collectionService).updateImage(COLLECTION_KEY, ITEM_ID, USER_ID, multipartFile);
        }
    }

    @Nested
    class ValidateItemTests {

        @Test
        void validateItem_shouldDelegateToCollectionService() {
            // Given
            ValidateItemResponse expectedResponse = ValidateItemResponse.builder()
                    .hasSimilarItems(true)
                    .similarCaps(List.of())
                    .build();

            when(collectionService.validateItem(COLLECTION_KEY, USER_ID, multipartFile))
                    .thenReturn(expectedResponse);

            // When
            ValidateItemResponse result = collectionFacadeService.validateItem(
                    principal, COLLECTION_KEY, multipartFile);

            // Then
            assertEquals(expectedResponse, result);
            verify(collectionService).validateItem(COLLECTION_KEY, USER_ID, multipartFile);
        }
    }

    @Nested
    class DeleteItemTests {

        @Test
        void deleteItem_shouldDelegateToCollectionService() {
            // When
            collectionFacadeService.deleteItem(principal, COLLECTION_KEY, ITEM_ID);

            // Then
            verify(collectionService).deleteItem(COLLECTION_KEY, ITEM_ID, USER_ID);
        }
    }

    @Nested
    class GetCollectionAvailableTagsTests {

        @Test
        void getCollectionAvailableTags_shouldDelegateToUserService() {
            // Given
            List<String> tags = List.of("brand", "country", "year");
            when(userService.getCollectionAvailableTags(USER_ID, COLLECTION_KEY)).thenReturn(tags);

            // When
            List<String> result = collectionFacadeService.getCollectionAvailableTags(principal, COLLECTION_KEY);

            // Then
            assertEquals(tags, result);
            verify(userService).getCollectionAvailableTags(USER_ID, COLLECTION_KEY);
        }
    }

    @Nested
    class IdentifyItemTests {

        @Test
        void identifyItem_shouldCheckRateLimitAndIdentify() {
            // Given
            RateLimitInfo rateLimitInfo = RateLimitInfo.of(20, 5);
            ItemIdentificationResponse identificationResponse = ItemIdentificationResponse.builder()
                    .primaryName("Coca Cola")
                    .description("A cola bottle cap")
                    .build();

            when(rateLimitService.checkAndIncrementIdentificationUsage(USER_ID)).thenReturn(rateLimitInfo);
            when(collectionService.identifyItem(USER_ID, multipartFile)).thenReturn(identificationResponse);

            // When
            ItemIdentificationResponse result = collectionFacadeService.identifyItem(principal, multipartFile);

            // Then
            assertNotNull(result);
            assertEquals("Coca Cola", result.getPrimaryName());
            assertEquals(rateLimitInfo, result.getRateLimit());
            verify(rateLimitService).checkAndIncrementIdentificationUsage(USER_ID);
            verify(collectionService).identifyItem(USER_ID, multipartFile);
        }

        @Test
        void identifyItem_shouldAttachUnlimitedRateLimitInfoForAdmin() {
            // Given
            RateLimitInfo rateLimitInfo = RateLimitInfo.unlimited();
            ItemIdentificationResponse identificationResponse = ItemIdentificationResponse.builder()
                    .primaryName("Pepsi")
                    .build();

            when(rateLimitService.checkAndIncrementIdentificationUsage(USER_ID)).thenReturn(rateLimitInfo);
            when(collectionService.identifyItem(USER_ID, multipartFile)).thenReturn(identificationResponse);

            // When
            ItemIdentificationResponse result = collectionFacadeService.identifyItem(principal, multipartFile);

            // Then
            assertEquals(-1, result.getRateLimit().getLimit());
            assertEquals(-1, result.getRateLimit().getRemaining());
        }
    }

    @Nested
    class ShareCollectionTests {

        @Test
        void shareCollection_shouldDelegateToShareService() {
            // Given
            ShareCollectionRequest request = ShareCollectionRequest.builder()
                    .email("friend@example.com")
                    .build();
            ShareCollectionResponse expectedResponse = ShareCollectionResponse.builder()
                    .message("Collection shared successfully")
                    .build();

            when(collectionShareService.shareCollection(USER_ID, COLLECTION_KEY, "friend@example.com"))
                    .thenReturn(expectedResponse);

            // When
            ShareCollectionResponse result = collectionFacadeService.shareCollection(
                    principal, COLLECTION_KEY, request);

            // Then
            assertEquals(expectedResponse, result);
            verify(collectionShareService).shareCollection(USER_ID, COLLECTION_KEY, "friend@example.com");
        }
    }

    @Nested
    class RevokeShareTests {

        @Test
        void revokeShare_shouldDelegateToShareService() {
            // Given
            String targetUserId = "target-user-id";

            // When
            collectionFacadeService.revokeShare(principal, COLLECTION_KEY, targetUserId);

            // Then
            verify(collectionShareService).revokeShare(USER_ID, COLLECTION_KEY, targetUserId);
        }
    }

    @Nested
    class GetCollectionSharesTests {

        @Test
        void getCollectionShares_shouldDelegateToShareService() {
            // Given
            List<CollectionShareResponse> shares = List.of(
                    CollectionShareResponse.builder()
                            .userId("user-1")
                            .email("user1@example.com")
                            .build()
            );
            when(collectionShareService.getSharesForCollection(USER_ID, COLLECTION_KEY)).thenReturn(shares);

            // When
            List<CollectionShareResponse> result = collectionFacadeService.getCollectionShares(
                    principal, COLLECTION_KEY);

            // Then
            assertEquals(shares, result);
            verify(collectionShareService).getSharesForCollection(USER_ID, COLLECTION_KEY);
        }
    }

    @Nested
    class GetSharedWithMeCollectionsTests {

        @Test
        void getSharedWithMeCollections_shouldDelegateToShareService() {
            // Given
            List<SharedCollectionResponse> sharedCollections = List.of(
                    SharedCollectionResponse.builder()
                            .collectionKey("shared-key")
                            .collectionName("Shared Collection")
                            .ownerUserId("owner-123")
                            .ownerName("Owner Name")
                            .build()
            );
            when(collectionShareService.getCollectionsSharedWithUser(USER_ID)).thenReturn(sharedCollections);

            // When
            List<SharedCollectionResponse> result = collectionFacadeService.getSharedWithMeCollections(principal);

            // Then
            assertEquals(sharedCollections, result);
            verify(collectionShareService).getCollectionsSharedWithUser(USER_ID);
        }
    }

    @Nested
    class DeleteCollectionTests {

        @Test
        void deleteCollection_shouldRemoveSharesAndInitiateAsyncDeletion() {
            // Given
            when(appProperties.getLegacyCollectionKey()).thenReturn("legacy-key");
            when(userService.ownsCollection(USER_ID, COLLECTION_KEY)).thenReturn(true);

            // When
            collectionFacadeService.deleteCollection(principal, COLLECTION_KEY);

            // Then - ownership verified, shares removed, then async deletion initiated
            verify(userService).ownsCollection(USER_ID, COLLECTION_KEY);
            verify(collectionShareService).removeCollectionAndAllShares(USER_ID, COLLECTION_KEY);
            verify(collectionService).deleteAllItemsInCollectionAsync(COLLECTION_KEY, USER_ID);
        }

        @Test
        void deleteCollection_shouldThrowWhenUserDoesNotOwnCollection() {
            // Given
            when(appProperties.getLegacyCollectionKey()).thenReturn("legacy-key");
            when(userService.ownsCollection(USER_ID, COLLECTION_KEY)).thenReturn(false);

            // When / Then
            org.junit.jupiter.api.Assertions.assertThrows(
                    com.km.bottlecapcollector.api.handler.exception.AppForbiddenException.class,
                    () -> collectionFacadeService.deleteCollection(principal, COLLECTION_KEY)
            );

            // Verify no deletion occurred
            verify(collectionShareService, never()).removeCollectionAndAllShares(any(), any());
            verify(collectionService, never()).deleteAllItemsInCollectionAsync(any(), any());
        }

        @Test
        void deleteCollection_shouldThrowWhenDeletingLegacyCollection() {
            // Given
            when(appProperties.getLegacyCollectionKey()).thenReturn(COLLECTION_KEY);

            // When / Then
            org.junit.jupiter.api.Assertions.assertThrows(
                    com.km.bottlecapcollector.api.handler.exception.AppBadRequestException.class,
                    () -> collectionFacadeService.deleteCollection(principal, COLLECTION_KEY)
            );

            // Verify no deletion occurred
            verify(userService, never()).ownsCollection(any(), any());
            verify(collectionShareService, never()).removeCollectionAndAllShares(any(), any());
            verify(collectionService, never()).deleteAllItemsInCollectionAsync(any(), any());
        }
    }
}
