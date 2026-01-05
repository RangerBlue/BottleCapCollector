package com.km.bottlecapcollector.firestore.service;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.km.bottlecapcollector.firestore.document.FirestoreVisionMetadata;
import com.km.bottlecapcollector.firestore.document.FirestoreVisionMetadata.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for analyzing images using Google Cloud Vision API.
 */
@Service
@Slf4j
public class VisionApiService {

    private final ImageAnnotatorClient visionClient;

    public VisionApiService(ImageAnnotatorClient visionClient) {
        this.visionClient = visionClient;
    }

    /**
     * Analyzes an image from Cloud Storage and returns vision metadata.
     *
     * @param gcsUri the Cloud Storage URI (gs://bucket/object)
     * @return FirestoreVisionMetadata with analysis results
     */
    public FirestoreVisionMetadata analyzeImageFromGcs(String gcsUri) {
        log.info("Analyzing image from GCS: {}", gcsUri);

        ImageSource imageSource = ImageSource.newBuilder()
                .setGcsImageUri(gcsUri)
                .build();
        Image image = Image.newBuilder()
                .setSource(imageSource)
                .build();

        return analyzeImage(image);
    }

    /**
     * Analyzes an image from a MultipartFile and returns vision metadata.
     *
     * @param file the image file
     * @return FirestoreVisionMetadata with analysis results
     * @throws IOException if reading file fails
     */
    public FirestoreVisionMetadata analyzeImageFromFile(MultipartFile file) throws IOException {
        log.info("Analyzing image from file: {}", file.getOriginalFilename());

        ByteString imgBytes = ByteString.copyFrom(file.getBytes());
        Image image = Image.newBuilder()
                .setContent(imgBytes)
                .build();

        return analyzeImage(image);
    }

    /**
     * Analyzes an image from raw bytes and returns vision metadata.
     *
     * @param imageBytes the image bytes
     * @return FirestoreVisionMetadata with analysis results
     */
    public FirestoreVisionMetadata analyzeImageFromBytes(byte[] imageBytes) {
        log.info("Analyzing image from bytes");

        ByteString imgBytes = ByteString.copyFrom(imageBytes);
        Image image = Image.newBuilder()
                .setContent(imgBytes)
                .build();

        return analyzeImage(image);
    }

    private FirestoreVisionMetadata analyzeImage(Image image) {
        // Build feature requests
        List<Feature> features = List.of(
                Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).setMaxResults(10).build(),
                Feature.newBuilder().setType(Feature.Type.IMAGE_PROPERTIES).build(),
                Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build(),
                Feature.newBuilder().setType(Feature.Type.LOGO_DETECTION).setMaxResults(5).build(),
                Feature.newBuilder().setType(Feature.Type.SAFE_SEARCH_DETECTION).build()
        );

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .setImage(image)
                .addAllFeatures(features)
                .build();

        BatchAnnotateImagesResponse response = visionClient.batchAnnotateImages(List.of(request));
        AnnotateImageResponse imageResponse = response.getResponsesList().get(0);

        if (imageResponse.hasError()) {
            log.error("Vision API error: {}", imageResponse.getError().getMessage());
            throw new RuntimeException("Vision API error: " + imageResponse.getError().getMessage());
        }

        return buildVisionMetadata(imageResponse);
    }

    private FirestoreVisionMetadata buildVisionMetadata(AnnotateImageResponse response) {
        FirestoreVisionMetadata.FirestoreVisionMetadataBuilder builder = FirestoreVisionMetadata.builder()
                .analyzedAt(Instant.now());

        // Process labels
        List<VisionLabel> labels = response.getLabelAnnotationsList().stream()
                .map(label -> VisionLabel.builder()
                        .description(label.getDescription())
                        .score((double) label.getScore())
                        .topicality((double) label.getTopicality())
                        .build())
                .collect(Collectors.toList());
        builder.labels(labels);

        // Calculate overall confidence from labels
        if (!labels.isEmpty()) {
            double avgConfidence = labels.stream()
                    .mapToDouble(VisionLabel::getScore)
                    .average()
                    .orElse(0.0);
            builder.overallConfidence(avgConfidence);
        }

        // Process dominant colors
        if (response.hasImagePropertiesAnnotation()) {
            ImageProperties imageProps = response.getImagePropertiesAnnotation();
            List<VisionColor> colors = imageProps.getDominantColors().getColorsList().stream()
                    .limit(5)
                    .map(colorInfo -> VisionColor.builder()
                            .red((int) colorInfo.getColor().getRed())
                            .green((int) colorInfo.getColor().getGreen())
                            .blue((int) colorInfo.getColor().getBlue())
                            .score((double) colorInfo.getScore())
                            .pixelFraction((double) colorInfo.getPixelFraction())
                            .build())
                    .collect(Collectors.toList());
            builder.dominantColors(colors);
        }

        // Process text
        if (!response.getTextAnnotationsList().isEmpty()) {
            TextAnnotation fullTextAnnotation = response.getFullTextAnnotation();
            List<String> words = new ArrayList<>();

            // Skip the first one as it contains all text, rest are individual words
            for (int i = 1; i < response.getTextAnnotationsList().size(); i++) {
                words.add(response.getTextAnnotationsList().get(i).getDescription());
            }

            String language = null;
            if (fullTextAnnotation.getPagesCount() > 0 &&
                    fullTextAnnotation.getPages(0).getProperty().getDetectedLanguagesCount() > 0) {
                language = fullTextAnnotation.getPages(0).getProperty()
                        .getDetectedLanguages(0).getLanguageCode();
            }

            builder.textAnnotation(VisionText.builder()
                    .fullText(fullTextAnnotation.getText())
                    .words(words)
                    .language(language)
                    .build());
        }

        // Process logos
        if (!response.getLogoAnnotationsList().isEmpty()) {
            EntityAnnotation logo = response.getLogoAnnotationsList().get(0);
            builder.logoAnnotation(VisionLogo.builder()
                    .description(logo.getDescription())
                    .score((double) logo.getScore())
                    .build());
        }


        log.info("Vision analysis complete. Found {} labels", labels.size());
        return builder.build();
    }

    /**
     * Extracts tags from vision metadata (label descriptions with high confidence).
     *
     * @param metadata the vision metadata
     * @return list of tag strings
     */
    public List<String> extractTags(FirestoreVisionMetadata metadata) {
        if (metadata == null || metadata.getLabels() == null) {
            return List.of();
        }
        return metadata.getLabels().stream()
                .filter(label -> label.getScore() > 0.7) // Only high-confidence labels
                .map(VisionLabel::getDescription)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }
}
