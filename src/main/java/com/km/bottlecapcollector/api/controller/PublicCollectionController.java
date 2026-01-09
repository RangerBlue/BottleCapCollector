package com.km.bottlecapcollector.api.controller;

import com.km.bottlecapcollector.api.model.response.CollectionItemResponse;
import com.km.bottlecapcollector.api.model.response.CollectionItemSummary;
import com.km.bottlecapcollector.property.AppProperties;
import com.km.bottlecapcollector.service.CollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public controller for accessing legacy collection items without authentication.
 * Uses configured legacy user and collection key to serve items to unauthenticated users.
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Slf4j
public class PublicCollectionController {

    private final CollectionService collectionService;
    private final AppProperties appProperties;

    @GetMapping("/items")
    public ResponseEntity<@NotNull Page<@NotNull CollectionItemSummary>> getItems(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 10) Pageable pageable) {
        String collectionKey = appProperties.getLegacyCollectionKey();
        String userId = appProperties.getLegacyUserId();
        log.info("Public access: getting items from legacy collection {} with query '{}', page: {}, size: {}",
                collectionKey, query, pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(collectionService.searchItemsPaginated(collectionKey, query, userId, pageable));
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<@NotNull CollectionItemResponse> getItem(@PathVariable String id) {
        String collectionKey = appProperties.getLegacyCollectionKey();
        String userId = appProperties.getLegacyUserId();
        log.info("Public access: getting item {} from legacy collection {}", id, collectionKey);
        return ResponseEntity.ok(collectionService.getCollectionItem(collectionKey, id, userId));
    }
}
