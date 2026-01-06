package com.km.bottlecapcollector.api.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for user collection information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCollectionResponse {

    /**
     * UUID key used for API calls.
     */
    private String collectionKey;

    /**
     * Human-readable name for display.
     */
    private String collectionName;
}
