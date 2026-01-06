package com.km.bottlecapcollector.api.controller;

import com.km.bottlecapcollector.api.model.response.ValidateItemResponse;
import com.km.bottlecapcollector.api.model.response.CollectionItemResponse;
import com.km.bottlecapcollector.api.model.response.CollectionItemSummary;
import com.km.bottlecapcollector.api.model.request.CreateCollectionItemRequest;
import com.km.bottlecapcollector.api.model.request.UpdateCollectionItem;
import com.km.bottlecapcollector.cloud.service.FirestoreBottleCapService;
import com.km.bottlecapcollector.cloud.service.FirestoreUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing collection items in Firestore.
 * The {collectionName} path variable specifies which Firestore collection to use.
 * All endpoints require authentication and operate on the authenticated user's data.
 */
@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
@Slf4j
public class CollectionItemController {

    private final FirestoreBottleCapService firestoreBottleCapService;
    private final FirestoreUserService userService;

    private String getUserId(OAuth2AuthenticatedPrincipal principal) {
        return Optional.ofNullable(principal)
                .map(p -> p.getAttribute("sub"))
                .map(String.class::cast)
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));
    }

    /**
     * Gets the collection names for the authenticated user.
     * Example: GET /api/collections
     */
    @GetMapping
    public ResponseEntity<List<String>> getUserCollections(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        String userId = getUserId(principal);
        log.info("Getting collections for user: {}", userId);
        return ResponseEntity.ok(userService.getCollectionNames(userId));
    }

    @PostMapping(value = "/{collectionName}/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CollectionItemResponse> createItem(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionName,
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("request") CreateCollectionItemRequest request) {
        String userId = getUserId(principal);
        request.setUserId(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(firestoreBottleCapService.createCollectionItem(collectionName, request, file));
    }

    @GetMapping("/{collectionName}/items/{id}")
    public ResponseEntity<CollectionItemResponse> getItem(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionName,
            @PathVariable String id) {
        String userId = getUserId(principal);
        log.trace("User {} getting item {} from collection {}", userId, id, collectionName);
        return ResponseEntity.ok(firestoreBottleCapService.getCollectionItem(collectionName, id, userId));
    }

    @GetMapping("/{collectionName}/items")
    public ResponseEntity<Page<CollectionItemSummary>> getItems(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionName,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 10) Pageable pageable) {
        String userId = getUserId(principal);
        log.info("User {} getting items from collection {} with query '{}', page: {}, size: {}",
                userId, collectionName, query, pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(firestoreBottleCapService.searchItemsPaginated(collectionName, query, userId, pageable));
    }

    @PutMapping("/{collectionName}/items/{id}")
    public ResponseEntity<CollectionItemResponse> updateItem(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionName,
            @PathVariable String id,
            @RequestBody UpdateCollectionItem request) {
        String userId = getUserId(principal);
        log.info("User {} updating item {} in collection {}", userId, id, collectionName);
        return ResponseEntity.ok(firestoreBottleCapService.updateItem(collectionName, id, userId, request));
    }

    @PutMapping(value = "/{collectionName}/items/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CollectionItemResponse> updateImage(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionName,
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) throws IOException {
        String userId = getUserId(principal);
        log.info("User {} updating image for item {} in collection {}", userId, id, collectionName);
        return ResponseEntity.ok(firestoreBottleCapService.updateImage(collectionName, id, userId, file));
    }

    @PostMapping(value = "/{collectionName}/items/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ValidateItemResponse> validateItem(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionName,
            @RequestParam("file") MultipartFile file) throws IOException {
        String userId = getUserId(principal);
        log.info("User {} validating item in collection {}", userId, collectionName);
        return ResponseEntity.ok(firestoreBottleCapService.validateItem(collectionName, userId, file));
    }

    @DeleteMapping("/{collectionName}/items/{id}")
    public ResponseEntity<Void> deleteItem(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionName,
            @PathVariable String id) {
        String userId = getUserId(principal);
        log.info("User {} deleting item {} from collection {}", userId, id, collectionName);
        firestoreBottleCapService.deleteItem(collectionName, id, userId);
        return ResponseEntity.noContent().build();
    }
}
