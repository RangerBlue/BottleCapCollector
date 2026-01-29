package com.km.bottlecapcollector.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.km.bottlecapcollector.api.handler.ControllerExceptionHandler;
import com.km.bottlecapcollector.api.handler.exception.AppForbiddenException;
import com.km.bottlecapcollector.api.model.request.ShareCollectionRequest;
import com.km.bottlecapcollector.api.model.response.CollectionShareResponse;
import com.km.bottlecapcollector.api.model.response.ShareCollectionResponse;
import com.km.bottlecapcollector.api.model.response.SharedCollectionResponse;
import com.km.bottlecapcollector.service.CollectionFacadeService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CollectionShareControllerTests {

    private static final String COLLECTION_KEY = "collection-key-uuid";
    private static final String BASE_PATH = "/api/v1/collections";
    private static final String TARGET_USER_ID = "target-user-123";
    private static final String TARGET_EMAIL = "friend@example.com";

    private MockMvc mvc;

    @Mock
    private CollectionFacadeService collectionFacadeService;

    @Mock
    private OAuth2AuthenticatedPrincipal principal;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(OAuth2AuthenticatedPrincipal.class);
            }

            @Override
            public Object resolveArgument(@NotNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          @NotNull NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return principal;
            }
        };

        CollectionShareController controller = new CollectionShareController(collectionFacadeService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ControllerExceptionHandler())
                .setCustomArgumentResolvers(principalResolver)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Nested
    class ShareCollectionTests {

        @Test
        void shareCollection_shouldReturn201WhenSuccessful() throws Exception {
            // Given
            ShareCollectionRequest request = ShareCollectionRequest.builder()
                    .email(TARGET_EMAIL)
                    .build();
            ShareCollectionResponse response = ShareCollectionResponse.builder()
                    .message("Collection shared successfully with " + TARGET_EMAIL)
                    .shared(true)
                    .build();

            when(collectionFacadeService.shareCollection(any(OAuth2AuthenticatedPrincipal.class),
                    eq(COLLECTION_KEY), any(ShareCollectionRequest.class)))
                    .thenReturn(response);

            // When / Then
            mvc.perform(post(BASE_PATH + "/{collectionKey}/shares", COLLECTION_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message", is("Collection shared successfully with " + TARGET_EMAIL)))
                    .andExpect(jsonPath("$.shared", is(true)));

            verify(collectionFacadeService).shareCollection(any(), eq(COLLECTION_KEY), any(ShareCollectionRequest.class));
        }

        @Test
        void shareCollection_shouldReturn403WhenUserDoesNotOwnCollection() throws Exception {
            // Given
            ShareCollectionRequest request = ShareCollectionRequest.builder()
                    .email(TARGET_EMAIL)
                    .build();

            when(collectionFacadeService.shareCollection(any(OAuth2AuthenticatedPrincipal.class),
                    eq(COLLECTION_KEY), any(ShareCollectionRequest.class)))
                    .thenThrow(new AppForbiddenException("You do not own this collection"));

            // When / Then
            mvc.perform(post(BASE_PATH + "/{collectionKey}/shares", COLLECTION_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error", is("You do not own this collection")));
        }

        @Test
        void shareCollection_shouldReturn400WhenEmailMissing() throws Exception {
            // Given - request without email
            String requestJson = "{}";

            // When / Then - validation exception returns 400 with field errors
            mvc.perform(post(BASE_PATH + "/{collectionKey}/shares", COLLECTION_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error.message", is("Validation failed")))
                    .andExpect(jsonPath("$.error.fieldErrors.email", is("Email is required")));
        }
    }

    @Nested
    class RevokeShareTests {

        @Test
        void revokeShare_shouldReturn204WhenSuccessful() throws Exception {
            // When / Then
            mvc.perform(delete(BASE_PATH + "/{collectionKey}/shares/{targetUserId}",
                            COLLECTION_KEY, TARGET_USER_ID))
                    .andExpect(status().isNoContent());

            verify(collectionFacadeService).revokeShare(any(), eq(COLLECTION_KEY), eq(TARGET_USER_ID));
        }

        @Test
        void revokeShare_shouldReturn403WhenUserDoesNotOwnCollection() throws Exception {
            // Given
            doThrow(new AppForbiddenException("You do not own this collection"))
                    .when(collectionFacadeService).revokeShare(any(OAuth2AuthenticatedPrincipal.class),
                            eq(COLLECTION_KEY), eq(TARGET_USER_ID));

            // When / Then
            mvc.perform(delete(BASE_PATH + "/{collectionKey}/shares/{targetUserId}",
                            COLLECTION_KEY, TARGET_USER_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error", is("You do not own this collection")));
        }
    }

    @Nested
    class GetCollectionSharesTests {

        @Test
        void getCollectionShares_shouldReturnSharesList() throws Exception {
            // Given
            List<CollectionShareResponse> shares = List.of(
                    CollectionShareResponse.builder()
                            .userId("user-1")
                            .email("user1@example.com")
                            .sharedAt(Instant.parse("2026-01-15T10:00:00Z"))
                            .build(),
                    CollectionShareResponse.builder()
                            .userId("user-2")
                            .email("user2@example.com")
                            .sharedAt(Instant.parse("2026-01-20T10:00:00Z"))
                            .build()
            );

            when(collectionFacadeService.getCollectionShares(any(OAuth2AuthenticatedPrincipal.class),
                    eq(COLLECTION_KEY)))
                    .thenReturn(shares);

            // When / Then
            mvc.perform(get(BASE_PATH + "/{collectionKey}/shares", COLLECTION_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].userId", is("user-1")))
                    .andExpect(jsonPath("$[0].email", is("user1@example.com")))
                    .andExpect(jsonPath("$[1].userId", is("user-2")))
                    .andExpect(jsonPath("$[1].email", is("user2@example.com")));
        }

        @Test
        void getCollectionShares_shouldReturnEmptyListWhenNoShares() throws Exception {
            // Given
            when(collectionFacadeService.getCollectionShares(any(OAuth2AuthenticatedPrincipal.class),
                    eq(COLLECTION_KEY)))
                    .thenReturn(List.of());

            // When / Then
            mvc.perform(get(BASE_PATH + "/{collectionKey}/shares", COLLECTION_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void getCollectionShares_shouldReturn403WhenUserDoesNotOwnCollection() throws Exception {
            // Given
            when(collectionFacadeService.getCollectionShares(any(OAuth2AuthenticatedPrincipal.class),
                    eq(COLLECTION_KEY)))
                    .thenThrow(new AppForbiddenException("You do not own this collection"));

            // When / Then
            mvc.perform(get(BASE_PATH + "/{collectionKey}/shares", COLLECTION_KEY))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class GetSharedWithMeCollectionsTests {

        @Test
        void getSharedWithMeCollections_shouldReturnSharedCollectionsList() throws Exception {
            // Given
            List<SharedCollectionResponse> sharedCollections = List.of(
                    SharedCollectionResponse.builder()
                            .collectionKey("shared-key-1")
                            .collectionName("Friend's Collection")
                            .ownerUserId("friend-123")
                            .ownerName("Friend Name")
                            .sharedAt(Instant.parse("2026-01-10T10:00:00Z"))
                            .build(),
                    SharedCollectionResponse.builder()
                            .collectionKey("shared-key-2")
                            .collectionName("Another Collection")
                            .ownerUserId("other-456")
                            .ownerName("Other Name")
                            .sharedAt(Instant.parse("2026-01-12T10:00:00Z"))
                            .build()
            );

            when(collectionFacadeService.getSharedWithMeCollections(any(OAuth2AuthenticatedPrincipal.class)))
                    .thenReturn(sharedCollections);

            // When / Then
            mvc.perform(get(BASE_PATH + "/shared-with-me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].collectionKey", is("shared-key-1")))
                    .andExpect(jsonPath("$[0].collectionName", is("Friend's Collection")))
                    .andExpect(jsonPath("$[0].ownerUserId", is("friend-123")))
                    .andExpect(jsonPath("$[0].ownerName", is("Friend Name")))
                    .andExpect(jsonPath("$[1].collectionKey", is("shared-key-2")));
        }

        @Test
        void getSharedWithMeCollections_shouldReturnEmptyListWhenNothingShared() throws Exception {
            // Given
            when(collectionFacadeService.getSharedWithMeCollections(any(OAuth2AuthenticatedPrincipal.class)))
                    .thenReturn(List.of());

            // When / Then
            mvc.perform(get(BASE_PATH + "/shared-with-me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
