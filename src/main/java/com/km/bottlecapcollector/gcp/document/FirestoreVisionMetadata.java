package com.km.bottlecapcollector.gcp.document;

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
public class FirestoreVisionMetadata {

    private List<VisionLabel> labels;

    private List<VisionColor> dominantColors;

    private VisionText textAnnotation;

    private VisionLogo logoAnnotation;

    private Double overallConfidence;

    private Instant analyzedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VisionLabel {
        private String description;
        private Double score;
        private Double topicality;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VisionColor {
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
    public static class VisionText {
        private String fullText;
        private List<String> words;
        private String language;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VisionLogo {
        private String description;
        private Double score;
    }
}
