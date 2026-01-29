package com.km.bottlecapcollector.api.controller;

import com.km.bottlecapcollector.api.model.response.CollectionItemResponse;
import com.km.bottlecapcollector.api.model.response.CollectionItemSummary;
import com.km.bottlecapcollector.api.model.response.DeleteCollectionResponse;
import com.km.bottlecapcollector.api.model.response.ErrorResponse;
import com.km.bottlecapcollector.api.model.response.ItemIdentificationResponse;
import com.km.bottlecapcollector.api.model.response.UserCollectionResponse;
import com.km.bottlecapcollector.api.model.response.ValidateItemResponse;
import com.km.bottlecapcollector.api.model.request.CreateCollectionItemRequest;
import com.km.bottlecapcollector.api.model.request.UpdateCollectionItem;
import com.km.bottlecapcollector.service.CollectionFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
@Tag(name = "Collection Items", description = "Operations for managing collection items")
public class CollectionItemController {

    private final CollectionFacadeService collectionFacadeService;

    @GetMapping
    public ResponseEntity<@NotNull List<UserCollectionResponse>> getUserCollections(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(collectionFacadeService.getUserCollections(principal));
    }

    @Operation(summary = "Delete a collection",
            description = "Deletes a collection and all its items asynchronously. Also revokes all shares for this collection. " +
                    "The collection is immediately removed from the user's list and shares are revoked. " +
                    "Items are deleted in the background.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Collection deletion initiated",
                    content = @Content(schema = @Schema(implementation = DeleteCollectionResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - user does not own the collection",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-01-25T12:00:00Z",
                                      "status": 403,
                                      "error": "You do not own this collection"
                                    }
                                    """)))
    })
    @DeleteMapping("/{collectionKey}")
    public ResponseEntity<@NotNull DeleteCollectionResponse> deleteCollection(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionKey) {
        collectionFacadeService.deleteCollection(principal, collectionKey);
        return ResponseEntity.accepted().body(DeleteCollectionResponse.builder()
                .message("Collection deletion initiated. Items are being deleted in the background.")
                .build());
    }

    @Operation(summary = "Create a new collection item", description = "Creates a new item in the specified collection with an image file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Item created successfully",
                    content = @Content(schema = @Schema(implementation = CollectionItemResponse.class))),
            @ApiResponse(responseCode = "429", description = "Item limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-01-25T12:00:00Z",
                                      "status": 429,
                                      "error": {
                                        "message": "Item limit exceeded. You have 100 items, maximum allowed is 100.",
                                        "limitType": "items",
                                        "limit": 100,
                                        "used": 100,
                                        "remaining": 0
                                      }
                                    }
                                    """)))
    })
    @PostMapping(value = "/{collectionKey}/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<@NotNull CollectionItemResponse> createItem(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionKey,
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("request") CreateCollectionItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collectionFacadeService.createItem(principal, collectionKey, request, file));
    }

    @Operation(summary = "Get a collection item",
            description = "Gets a specific item from a collection. Works for both owned collections and collections shared with the user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CollectionItemResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - user does not own the collection and it is not shared with them",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-01-25T12:00:00Z",
                                      "status": 403,
                                      "error": "You do not have access to this collection"
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{collectionKey}/items/{id}")
    public ResponseEntity<@NotNull CollectionItemResponse> getItem(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionKey,
            @PathVariable String id) {
        return ResponseEntity.ok(collectionFacadeService.getItem(principal, collectionKey, id));
    }

    @Operation(summary = "Get collection items",
            description = "Gets paginated items from a collection with optional search. Works for both owned collections and collections shared with the user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Items retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - user does not own the collection and it is not shared with them",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-01-25T12:00:00Z",
                                      "status": 403,
                                      "error": "You do not have access to this collection"
                                    }
                                    """)))
    })
    @GetMapping("/{collectionKey}/items")
    public ResponseEntity<@NotNull Page<@NotNull CollectionItemSummary>> getItems(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionKey,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(collectionFacadeService.getItems(principal, collectionKey, query, pageable));
    }

    @PutMapping("/{collectionKey}/items/{id}")
    public ResponseEntity<@NotNull CollectionItemResponse> updateItem(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionKey,
            @PathVariable String id,
            @RequestBody UpdateCollectionItem request) {
        return ResponseEntity.ok(collectionFacadeService.updateItem(principal, collectionKey, id, request));
    }

    @PutMapping(value = "/{collectionKey}/items/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<@NotNull CollectionItemResponse> updateImage(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionKey,
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(collectionFacadeService.updateImage(principal, collectionKey, id, file));
    }

    @PostMapping(value = "/{collectionKey}/items/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<@NotNull ValidateItemResponse> validateItem(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionKey,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(collectionFacadeService.validateItem(principal, collectionKey, file));
    }

    @DeleteMapping("/{collectionKey}/items/{id}")
    public ResponseEntity<@NotNull Void> deleteItem(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionKey,
            @PathVariable String id) {
        collectionFacadeService.deleteItem(principal, collectionKey, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{collectionKey}/available-tags")
    public ResponseEntity<@NotNull List<String>> getAvailableTags(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionKey) {
        return ResponseEntity.ok(collectionFacadeService.getCollectionAvailableTags(principal, collectionKey));
    }

    @Operation(summary = "Identify an item using AI",
            description = "Uses Gemini Vision AI to identify an item from an uploaded image. Rate-limited: USER role has daily limits, ADMIN has unlimited access.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item identified successfully",
                    content = @Content(schema = @Schema(implementation = ItemIdentificationResponse.class))),
            @ApiResponse(responseCode = "429", description = "Daily identification limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-01-25T12:00:00Z",
                                      "status": 429,
                                      "error": {
                                        "message": "Daily identification limit exceeded. Used 20 of 20 allowed calls.",
                                        "limitType": "identification",
                                        "limit": 20,
                                        "used": 20,
                                        "remaining": 0
                                      }
                                    }
                                    """)))
    })
    @PostMapping(value = "/items/identify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<@NotNull ItemIdentificationResponse> identifyItem(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(collectionFacadeService.identifyItem(principal, file));
    }
}
