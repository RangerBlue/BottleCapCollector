package com.km.bottlecapcollector.firestore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO for transferring FirestoreVisionMetadata data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirestoreVisionMetadataDto {

    private List<VisionLabelDto> labels;

    private List<VisionColorDto> dominantColors;

    private VisionTextDto textAnnotation;

    private VisionLogoDto logoAnnotation;

    private Double overallConfidence;

    private Instant analyzedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VisionLabelDto {
        private String description;
        private Double score;
        private Double topicality;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VisionColorDto {
        private Integer red;
        private Integer green;
        private Integer blue;
        private Double score;
        private Double pixelFraction;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VisionTextDto {
        private String fullText;
        private List<String> words;
        private String language;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VisionLogoDto {
        private String description;
        private Double score;
    }

}
