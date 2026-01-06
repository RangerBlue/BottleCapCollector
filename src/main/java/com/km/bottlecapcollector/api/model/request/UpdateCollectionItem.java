package com.km.bottlecapcollector.api.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for updating a bottle cap in Firestore.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCollectionItem {

    private String name;

    private String description;

    private List<String> tags;

    /**
     * Custom key-value tags (e.g., {"rank": "the best", "color": "blue"}).
     */
    private Map<String, String> customTags;
}
