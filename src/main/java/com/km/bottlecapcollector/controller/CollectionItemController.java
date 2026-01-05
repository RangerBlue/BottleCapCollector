package com.km.bottlecapcollector.controller;

import com.km.bottlecapcollector.gcp.dto.*;
import com.km.bottlecapcollector.gcp.service.FirestoreBottleCapService;
import com.km.bottlecapcollector.gcp.service.FirestoreUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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
        return principal.getAttribute("sub");
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
        return ResponseEntity.ok(firestoreBottleCapService.getCollectionItem(collectionName, id));
    }

    @GetMapping("/{collectionName}/items/paginated")
    public ResponseEntity<List<CollectionItemResponse>> getItemsPaginated(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionName,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        String userId = getUserId(principal);
        log.trace("User {} getting paginated items from collection {}", userId, collectionName);
        return ResponseEntity.ok(firestoreBottleCapService.getCollectionByTypePaginated(collectionName, limit, offset));
    }

    @GetMapping("/{collectionName}/items/count")
    public ResponseEntity<Long> getItemCount(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionName) {
        String userId = getUserId(principal);
        log.trace("User {} getting item count from collection {}", userId, collectionName);
        return ResponseEntity.ok(firestoreBottleCapService.getItemCount(collectionName));
    }

    /**
     * Searches items across multiple fields: name, description, tags, customTags,
     * and Vision API metadata (labels, text, logos).
     */
    @GetMapping("/{collectionName}/items/search")
    public ResponseEntity<List<CollectionItemResponse>> searchItems(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionName,
            @RequestParam String query) {
        String userId = getUserId(principal);
        log.info("User {} searching items with query '{}' in collection {}", userId, query, collectionName);
        return ResponseEntity.ok(firestoreBottleCapService.searchItems(collectionName, query));
    }

    @GetMapping("/{collectionName}/items/by-tag")
    public ResponseEntity<List<CollectionItemResponse>> getItemsByTag(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionName,
            @RequestParam String tag) {
        String userId = getUserId(principal);
        log.trace("User {} getting items by tag {} from collection {}", userId, tag, collectionName);
        return ResponseEntity.ok(firestoreBottleCapService.getItemsByTag(collectionName, tag));
    }

    @GetMapping("/{collectionName}/items/by-user")
    public ResponseEntity<List<CollectionItemResponse>> getItemsByUser(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionName) {
        String userId = getUserId(principal);
        log.trace("User {} getting their items from collection {}", userId, collectionName);
        return ResponseEntity.ok(firestoreBottleCapService.getItemsByUserId(collectionName, userId));
    }

    @PutMapping("/{collectionName}/items/{id}")
    public ResponseEntity<CollectionItemResponse> updateItem(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionName,
            @PathVariable String id,
            @RequestBody UpdateCollectionItem request) {
        String userId = getUserId(principal);
        log.info("User {} updating item {} in collection {}", userId, id, collectionName);
        return ResponseEntity.ok(firestoreBottleCapService.updateItem(collectionName, id, request));
    }

    @PutMapping(value = "/{collectionName}/items/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CollectionItemResponse> updateImage(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionName,
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) throws IOException {
        String userId = getUserId(principal);
        log.info("User {} updating image for item {} in collection {}", userId, id, collectionName);
        return ResponseEntity.ok(firestoreBottleCapService.updateImage(collectionName, id, file));
    }

    @PostMapping(value = "/{collectionName}/items/{id}/image/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CollectionItemResponse> uploadImage(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionName,
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) throws IOException {
        String userId = getUserId(principal);
        log.info("User {} uploading image for item {} in collection {}", userId, id, collectionName);
        return ResponseEntity.ok(firestoreBottleCapService.uploadImage(collectionName, id, file));
    }

    @PostMapping(value = "/{collectionName}/items/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CheckCapResponse> validateItem(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionName,
            @RequestParam("file") MultipartFile file) throws IOException {
        String userId = getUserId(principal);
        log.info("User {} validating item in collection {}", userId, collectionName);
        return ResponseEntity.ok(firestoreBottleCapService.validateItem(collectionName, file));
    }

    @DeleteMapping("/{collectionName}/items/{id}")
    public ResponseEntity<Void> deleteItem(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionName,
            @PathVariable String id) {
        String userId = getUserId(principal);
        log.info("User {} deleting item {} from collection {}", userId, id, collectionName);
        firestoreBottleCapService.deleteItem(collectionName, id);
        return ResponseEntity.noContent().build();
    }
}
