package com.km.bottlecapcollector.gcp.dto;

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
public class CheckCapResponse {

    private String temporaryCapId;

    private List<SimilarCapDto> similarCaps;

    private boolean hasSimilarCaps;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SimilarCapDto {

        private String id;

        private String name;

        private String imageUrl;

        private Double similarityScore;
    }
}
