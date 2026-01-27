package com.km.bottlecapcollector.api.controller;

import com.km.bottlecapcollector.api.model.request.ShareCollectionRequest;
import com.km.bottlecapcollector.api.model.response.CollectionShareResponse;
import com.km.bottlecapcollector.api.model.response.ErrorResponse;
import com.km.bottlecapcollector.api.model.response.ShareCollectionResponse;
import com.km.bottlecapcollector.api.model.response.SharedCollectionResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
@Tag(name = "Collection Sharing", description = "Operations for sharing collections between users")
public class CollectionShareController {

    private final CollectionFacadeService collectionFacadeService;

    @Operation(summary = "Share a collection",
            description = "Share a collection with another user by email. Only the collection owner can share it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Share request processed",
                    content = @Content(schema = @Schema(implementation = ShareCollectionResponse.class))),
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
    @PostMapping("/{collectionKey}/shares")
    public ResponseEntity<@NotNull ShareCollectionResponse> shareCollection(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionKey,
            @Valid @RequestBody ShareCollectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collectionFacadeService.shareCollection(principal, collectionKey, request));
    }

    @Operation(summary = "Revoke a share",
            description = "Revoke access to a collection from a user. Only the collection owner can revoke shares.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Share revoked successfully"),
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
    @DeleteMapping("/{collectionKey}/shares/{targetUserId}")
    public ResponseEntity<@NotNull Void> revokeShare(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionKey,
            @PathVariable String targetUserId) {
        collectionFacadeService.revokeShare(principal, collectionKey, targetUserId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List shares for a collection",
            description = "Get all users a collection is shared with. Only the collection owner can view shares.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shares retrieved successfully"),
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
    @GetMapping("/{collectionKey}/shares")
    public ResponseEntity<@NotNull List<CollectionShareResponse>> getCollectionShares(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @PathVariable String collectionKey) {
        return ResponseEntity.ok(collectionFacadeService.getCollectionShares(principal, collectionKey));
    }

    @Operation(summary = "Get collections shared with me",
            description = "Get all collections that have been shared with the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shared collections retrieved successfully")
    })
    @GetMapping("/shared-with-me")
    public ResponseEntity<@NotNull List<SharedCollectionResponse>> getSharedWithMeCollections(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(collectionFacadeService.getSharedWithMeCollections(principal));
    }
}
