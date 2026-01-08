package com.km.bottlecapcollector.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.km.bottlecapcollector.api.handler.ControllerExceptionHandler;
import com.km.bottlecapcollector.api.model.request.CreateCollectionItemRequest;
import com.km.bottlecapcollector.api.model.request.UpdateCollectionItem;
import com.km.bottlecapcollector.api.model.response.CollectionItemResponse;
import com.km.bottlecapcollector.api.model.response.CollectionItemSummary;
import com.km.bottlecapcollector.api.model.response.ImageResponse;
import com.km.bottlecapcollector.api.model.response.UserCollectionResponse;
import com.km.bottlecapcollector.api.model.response.ValidateItemResponse;
import com.km.bottlecapcollector.cloud.database.exception.FirestoreDocumentNotFoundException;
import com.km.bottlecapcollector.service.CollectionFacadeService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CollectionItemControllerTests {

    private static final String COLLECTION_KEY = "collection-key-uuid";
    private static final String COLLECTION_NAME = "My Collection";
    private static final String ITEM_ID = "item-456";
    private static final String BASE_PATH = "/api/v1/collections";

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

        CollectionItemController controller = new CollectionItemController(collectionFacadeService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ControllerExceptionHandler())
                .setCustomArgumentResolvers(principalResolver, new PageableHandlerMethodArgumentResolver())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void getUserCollections_shouldReturnCollections() throws Exception {
        // Given
        List<UserCollectionResponse> collections = List.of(
                UserCollectionResponse.builder()
                        .collectionKey(COLLECTION_KEY)
                        .collectionName(COLLECTION_NAME)
                        .build(),
                UserCollectionResponse.builder()
                        .collectionKey("key-2")
                        .collectionName("Second Collection")
                        .build()
        );
        when(collectionFacadeService.getUserCollections(any(OAuth2AuthenticatedPrincipal.class)))
                .thenReturn(collections);

        // When / Then
        mvc.perform(get(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].collectionKey", is(COLLECTION_KEY)))
                .andExpect(jsonPath("$[0].collectionName", is(COLLECTION_NAME)))
                .andExpect(jsonPath("$[1].collectionKey", is("key-2")))
                .andExpect(jsonPath("$[1].collectionName", is("Second Collection")));
    }

    @Test
    void getUserCollections_shouldReturnEmptyListWhenNoCollections() throws Exception {
        // Given
        when(collectionFacadeService.getUserCollections(any(OAuth2AuthenticatedPrincipal.class)))
                .thenReturn(List.of());

        // When / Then
        mvc.perform(get(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void createItem_shouldReturnCreatedItem() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test image".getBytes());

        CreateCollectionItemRequest request = CreateCollectionItemRequest.builder()
                .name("New Cap")
                .description("A nice cap")
                .build();
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", objectMapper.writeValueAsBytes(request));

        CollectionItemResponse response = CollectionItemResponse.builder()
                .id(ITEM_ID)
                .name("New Cap")
                .description("A nice cap")
                .collectionKey(COLLECTION_KEY)
                .collectionName(COLLECTION_NAME)
                .build();

        when(collectionFacadeService.createItem(any(OAuth2AuthenticatedPrincipal.class),
                eq(COLLECTION_KEY), any(CreateCollectionItemRequest.class), any()))
                .thenReturn(response);

        // When / Then
        mvc.perform(multipart(BASE_PATH + "/{collectionKey}/items", COLLECTION_KEY)
                        .file(file)
                        .file(requestPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(ITEM_ID)))
                .andExpect(jsonPath("$.name", is("New Cap")))
                .andExpect(jsonPath("$.description", is("A nice cap")));
    }

    @Test
    void getItem_shouldReturnItem() throws Exception {
        // Given
        CollectionItemResponse response = CollectionItemResponse.builder()
                .id(ITEM_ID)
                .name("Test Cap")
                .description("Description")
                .collectionKey(COLLECTION_KEY)
                .collectionName(COLLECTION_NAME)
                .image(ImageResponse.builder()
                        .objectName("item-456/image.jpg")
                        .signedUrl("https://signed-url.com")
                        .build())
                .build();

        when(collectionFacadeService.getItem(any(OAuth2AuthenticatedPrincipal.class),
                eq(COLLECTION_KEY), eq(ITEM_ID))).thenReturn(response);

        // When / Then
        mvc.perform(get(BASE_PATH + "/{collectionKey}/items/{id}", COLLECTION_KEY, ITEM_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(ITEM_ID)))
                .andExpect(jsonPath("$.name", is("Test Cap")))
                .andExpect(jsonPath("$.collectionName", is(COLLECTION_NAME)))
                .andExpect(jsonPath("$.image.signedUrl", is("https://signed-url.com")));
    }

    @Test
    void getItem_shouldReturn404WhenNotFound() throws Exception {
        // Given
        when(collectionFacadeService.getItem(any(OAuth2AuthenticatedPrincipal.class),
                eq(COLLECTION_KEY), eq(ITEM_ID)))
                .thenThrow(new FirestoreDocumentNotFoundException(COLLECTION_KEY, ITEM_ID));

        // When / Then
        mvc.perform(get(BASE_PATH + "/{collectionKey}/items/{id}", COLLECTION_KEY, ITEM_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getItems_shouldReturnPaginatedItems() throws Exception {
        // Given
        List<CollectionItemSummary> summaries = List.of(
                CollectionItemSummary.builder()
                        .id("item-1")
                        .name("Cap 1")
                        .signedUrl("https://url1.com")
                        .build(),
                CollectionItemSummary.builder()
                        .id("item-2")
                        .name("Cap 2")
                        .signedUrl("https://url2.com")
                        .build()
        );
        Page<@NotNull CollectionItemSummary> page =
                new PageImpl<>(summaries, PageRequest.of(0, 10), 2);

        when(collectionFacadeService.getItems(any(OAuth2AuthenticatedPrincipal.class),
                eq(COLLECTION_KEY), eq(null), any()))
                .thenReturn(page);

        // When / Then
        mvc.perform(get(BASE_PATH + "/{collectionKey}/items", COLLECTION_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is("item-1")))
                .andExpect(jsonPath("$.content[0].name", is("Cap 1")))
                .andExpect(jsonPath("$.content[1].id", is("item-2")))
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    void getItems_shouldPassQueryParameter() throws Exception {
        // Given
        String query = "beer";
        Page<CollectionItemSummary> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(collectionFacadeService.getItems(any(OAuth2AuthenticatedPrincipal.class),
                eq(COLLECTION_KEY), eq(query), any()))
                .thenReturn(page);

        // When / Then
        mvc.perform(get(BASE_PATH + "/{collectionKey}/items", COLLECTION_KEY)
                        .param("query", query)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(collectionFacadeService).getItems(any(OAuth2AuthenticatedPrincipal.class),
                eq(COLLECTION_KEY), eq(query), any());
    }

    @Test
    void getItems_shouldPassPaginationParameters() throws Exception {
        // Given
        Page<CollectionItemSummary> page = new PageImpl<>(List.of(), PageRequest.of(2, 20), 0);

        when(collectionFacadeService.getItems(any(OAuth2AuthenticatedPrincipal.class),
                eq(COLLECTION_KEY), eq(null), any()))
                .thenReturn(page);

        // When / Then
        mvc.perform(get(BASE_PATH + "/{collectionKey}/items", COLLECTION_KEY)
                        .param("page", "2")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void updateItem_shouldReturnUpdatedItem() throws Exception {
        // Given
        UpdateCollectionItem request = UpdateCollectionItem.builder()
                .name("Updated Name")
                .description("Updated Description")
                .build();

        CollectionItemResponse response = CollectionItemResponse.builder()
                .id(ITEM_ID)
                .name("Updated Name")
                .description("Updated Description")
                .collectionKey(COLLECTION_KEY)
                .build();

        when(collectionFacadeService.updateItem(any(OAuth2AuthenticatedPrincipal.class),
                eq(COLLECTION_KEY), eq(ITEM_ID), any(UpdateCollectionItem.class)))
                .thenReturn(response);

        // When / Then
        mvc.perform(put(BASE_PATH + "/{collectionKey}/items/{id}", COLLECTION_KEY, ITEM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(ITEM_ID)))
                .andExpect(jsonPath("$.name", is("Updated Name")))
                .andExpect(jsonPath("$.description", is("Updated Description")));
    }

    @Test
    void updateItem_shouldReturn404WhenNotFound() throws Exception {
        // Given
        UpdateCollectionItem request = UpdateCollectionItem.builder()
                .name("Updated Name")
                .build();

        when(collectionFacadeService.updateItem(any(OAuth2AuthenticatedPrincipal.class),
                eq(COLLECTION_KEY), eq(ITEM_ID), any(UpdateCollectionItem.class)))
                .thenThrow(new FirestoreDocumentNotFoundException(COLLECTION_KEY, ITEM_ID));

        // When / Then
        mvc.perform(put(BASE_PATH + "/{collectionKey}/items/{id}", COLLECTION_KEY, ITEM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateImage_shouldReturnUpdatedItem() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "new-image.jpg", "image/jpeg", "new image content".getBytes());

        CollectionItemResponse response = CollectionItemResponse.builder()
                .id(ITEM_ID)
                .name("Test Cap")
                .image(ImageResponse.builder()
                        .objectName("item-456/new-image.jpg")
                        .signedUrl("https://new-signed-url.com")
                        .build())
                .build();

        when(collectionFacadeService.updateImage(any(OAuth2AuthenticatedPrincipal.class),
                eq(COLLECTION_KEY), eq(ITEM_ID), any()))
                .thenReturn(response);

        // When / Then
        mvc.perform(multipart(BASE_PATH + "/{collectionKey}/items/{id}/image", COLLECTION_KEY, ITEM_ID)
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(ITEM_ID)))
                .andExpect(jsonPath("$.image.signedUrl", is("https://new-signed-url.com")));
    }

    @Test
    void validateItem_shouldReturnValidationResult() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test image".getBytes());

        ValidateItemResponse response = ValidateItemResponse.builder()
                .hasSimilarItems(true)
                .similarCaps(List.of(
                        ValidateItemResponse.SimilarItem.builder()
                                .id("similar-1")
                                .name("Similar Cap")
                                .similarityScore(0.95)
                                .imageUrl("https://image-url.com")
                                .build()
                ))
                .build();

        when(collectionFacadeService.validateItem(any(OAuth2AuthenticatedPrincipal.class),
                eq(COLLECTION_KEY), any())).thenReturn(response);

        // When / Then
        mvc.perform(multipart(BASE_PATH + "/{collectionKey}/items/validate", COLLECTION_KEY)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasSimilarItems", is(true)))
                .andExpect(jsonPath("$.similarCaps", hasSize(1)))
                .andExpect(jsonPath("$.similarCaps[0].id", is("similar-1")))
                .andExpect(jsonPath("$.similarCaps[0].similarityScore", is(0.95)));
    }

    @Test
    void validateItem_shouldReturnNoSimilarItems() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test image".getBytes());

        ValidateItemResponse response = ValidateItemResponse.builder()
                .hasSimilarItems(false)
                .similarCaps(List.of())
                .build();

        when(collectionFacadeService.validateItem(any(OAuth2AuthenticatedPrincipal.class),
                eq(COLLECTION_KEY), any())).thenReturn(response);

        // When / Then
        mvc.perform(multipart(BASE_PATH + "/{collectionKey}/items/validate", COLLECTION_KEY)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasSimilarItems", is(false)))
                .andExpect(jsonPath("$.similarCaps", hasSize(0)));
    }

    @Test
    void deleteItem_shouldReturnNoContent() throws Exception {
        // When / Then
        mvc.perform(delete(BASE_PATH + "/{collectionKey}/items/{id}", COLLECTION_KEY, ITEM_ID))
                .andExpect(status().isNoContent());

        verify(collectionFacadeService).deleteItem(any(OAuth2AuthenticatedPrincipal.class),
                eq(COLLECTION_KEY), eq(ITEM_ID));
    }

    @Test
    void deleteItem_shouldReturn404WhenNotFound() throws Exception {
        // Given
        doThrow(new FirestoreDocumentNotFoundException(COLLECTION_KEY, ITEM_ID))
                .when(collectionFacadeService).deleteItem(any(OAuth2AuthenticatedPrincipal.class),
                        eq(COLLECTION_KEY), eq(ITEM_ID));

        // When / Then
        mvc.perform(delete(BASE_PATH + "/{collectionKey}/items/{id}", COLLECTION_KEY, ITEM_ID))
                .andExpect(status().isNotFound());
    }
}
