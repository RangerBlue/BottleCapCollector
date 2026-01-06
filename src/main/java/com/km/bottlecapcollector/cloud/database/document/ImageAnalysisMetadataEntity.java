package com.km.bottlecapcollector.cloud.database.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageAnalysisMetadataEntity {

    private List<FirestoreImageLabel> imageLabels;
    private List<FirestoreImageColor> dominantColors;
    private FirestoreImageText textAnnotation;
    private FirestoreImageLogo logoAnnotation;
    private Double overallConfidence;
    private Instant analyzedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FirestoreImageLabel {
        private String description;
        private Double score;
        private Double topicality;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FirestoreImageColor {
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
    public static class FirestoreImageText {
        private String fullText;
        private List<String> words;
        private String language;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FirestoreImageLogo {
        private String description;
        private Double score;
    }
}
