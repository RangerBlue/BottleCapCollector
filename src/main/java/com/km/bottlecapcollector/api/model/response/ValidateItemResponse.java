package com.km.bottlecapcollector.api.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for the checkCap operation containing the temporary cap ID
 * and a list of similar caps found in the collection.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateItemResponse {

    private List<SimilarItem> similarCaps;

    private boolean hasSimilarItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SimilarItem {

        private String id;

        private String name;

        private String imageUrl;

        private Double similarityScore;
    }
}
