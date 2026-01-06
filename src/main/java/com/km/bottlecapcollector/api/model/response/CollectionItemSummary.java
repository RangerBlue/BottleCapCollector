package com.km.bottlecapcollector.api.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Lightweight DTO for paginated list responses.
 * Contains only essential fields for displaying items in a list view.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionItemSummary {

    private String id;

    private String name;

    private String signedUrl;

    private String collectionKey;

    private String collectionName;

    private String objectName;

    private Map<String, String> customTags;
}
