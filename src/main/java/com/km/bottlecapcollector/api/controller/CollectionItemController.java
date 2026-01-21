package com.km.bottlecapcollector.api.controller;

import com.km.bottlecapcollector.api.model.response.UserCollectionResponse;
import com.km.bottlecapcollector.api.model.response.ValidateItemResponse;
import com.km.bottlecapcollector.api.model.response.CollectionItemResponse;
import com.km.bottlecapcollector.api.model.response.CollectionItemSummary;
import com.km.bottlecapcollector.api.model.request.CreateCollectionItemRequest;
import com.km.bottlecapcollector.api.model.request.UpdateCollectionItem;
import com.km.bottlecapcollector.service.CollectionFacadeService;
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
public class CollectionItemController {

    private final CollectionFacadeService collectionFacadeService;

    @GetMapping
    public ResponseEntity<@NotNull List<UserCollectionResponse>> getUserCollections(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(collectionFacadeService.getUserCollections(principal));
    }

    @PostMapping(value = "/{collectionKey}/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<@NotNull CollectionItemResponse> createItem(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionKey,
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("request") CreateCollectionItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collectionFacadeService.createItem(principal, collectionKey, request, file));
    }

    @GetMapping("/{collectionKey}/items/{id}")
    public ResponseEntity<@NotNull CollectionItemResponse> getItem(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionKey,
            @PathVariable String id) {
        return ResponseEntity.ok(collectionFacadeService.getItem(principal, collectionKey, id));
    }

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
}
