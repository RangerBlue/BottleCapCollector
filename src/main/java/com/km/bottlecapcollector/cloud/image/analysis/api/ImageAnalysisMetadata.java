package com.km.bottlecapcollector.cloud.image.analysis.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Firestore embedded document representing Google Vision API metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageAnalysisMetadata {

    private List<ImageLabel> imageLabels;

    private List<ImageColor> dominantColors;

    private ImageText textAnnotation;

    private ImageLogo logoAnnotation;

    private Double overallConfidence;

    private Instant analyzedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImageLabel {
        private String description;
        private Double score;
        private Double topicality;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImageColor {
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
    public static class ImageText {
        private String fullText;
        private List<String> words;
        private String language;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImageLogo {
        private String description;
        private Double score;
    }
}
