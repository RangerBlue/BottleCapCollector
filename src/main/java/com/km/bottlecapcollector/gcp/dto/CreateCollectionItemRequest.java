package com.km.bottlecapcollector.gcp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a new bottle cap in Firestore.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCollectionItemRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    /**
     * Simple string tags.
     */
    private List<String> tags;

    /**
     * Custom key-value tags (e.g., {"rank": "the best", "color": "blue"}).
     */
    private Map<String, String> customTags;

    private String userId;
}
