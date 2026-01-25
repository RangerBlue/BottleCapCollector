package com.km.bottlecapcollector.cloud.image.analysis.vision;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.ColorInfo;
import com.google.cloud.vision.v1.DominantColorsAnnotation;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageProperties;
import com.google.rpc.Status;
import com.google.type.Color;
import com.km.bottlecapcollector.cloud.image.analysis.api.ImageAnalysisMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisionApiServiceTests {

    @Mock
    private ImageAnnotatorClient visionClient;

    @Mock
    private MultipartFile multipartFile;

    private VisionApiService visionApiService;

    @BeforeEach
    void setUp() {
        visionApiService = new VisionApiService(visionClient);
    }

    @Test
    void analyzeImageFromGcs_shouldReturnMetadataWithLabels() {
        // Given
        String gcsUri = "gs://test-bucket/image.jpg";
        EntityAnnotation label = EntityAnnotation.newBuilder()
                .setDescription("bottle cap")
                .setScore(0.95f)
                .setTopicality(0.9f)
                .build();

        AnnotateImageResponse imageResponse = AnnotateImageResponse.newBuilder()
                .addLabelAnnotations(label)
                .build();

        BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
                .addResponses(imageResponse)
                .build();

        when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);

        // When
        ImageAnalysisMetadata result = visionApiService.analyzeImageFromGcs(gcsUri);

        // Then
        assertNotNull(result);
        assertNotNull(result.getImageLabels());
        assertEquals(1, result.getImageLabels().size());
        assertEquals("bottle cap", result.getImageLabels().get(0).getDescription());
        assertEquals(0.95, result.getImageLabels().get(0).getScore(), 0.01);
        assertEquals(0.9, result.getImageLabels().get(0).getTopicality(), 0.01);
        assertNotNull(result.getAnalyzedAt());
    }

    @Test
    void analyzeImageFromFile_shouldReturnMetadata() throws IOException {
        // Given
        byte[] fileContent = "test image content".getBytes();
        when(multipartFile.getBytes()).thenReturn(fileContent);
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");

        EntityAnnotation label = EntityAnnotation.newBuilder()
                .setDescription("label1")
                .setScore(0.8f)
                .setTopicality(0.7f)
                .build();

        AnnotateImageResponse imageResponse = AnnotateImageResponse.newBuilder()
                .addLabelAnnotations(label)
                .build();

        BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
                .addResponses(imageResponse)
                .build();

        when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);

        // When
        ImageAnalysisMetadata result = visionApiService.analyzeImageFromFile(multipartFile);

        // Then
        assertNotNull(result);
        assertNotNull(result.getImageLabels());
        assertEquals(1, result.getImageLabels().size());
    }

    @Test
    void analyzeImageFromBytes_shouldReturnMetadata() {
        // Given
        byte[] imageBytes = "test image bytes".getBytes();

        EntityAnnotation label = EntityAnnotation.newBuilder()
                .setDescription("test label")
                .setScore(0.75f)
                .setTopicality(0.65f)
                .build();

        AnnotateImageResponse imageResponse = AnnotateImageResponse.newBuilder()
                .addLabelAnnotations(label)
                .build();

        BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
                .addResponses(imageResponse)
                .build();

        when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);

        // When
        ImageAnalysisMetadata result = visionApiService.analyzeImageFromBytes(imageBytes);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getImageLabels().size());
        assertEquals("test label", result.getImageLabels().get(0).getDescription());
    }

    @Test
    void analyzeImage_shouldCalculateOverallConfidence() {
        // Given
        String gcsUri = "gs://test-bucket/image.jpg";
        EntityAnnotation label1 = EntityAnnotation.newBuilder()
                .setDescription("label1")
                .setScore(0.9f)
                .setTopicality(0.8f)
                .build();
        EntityAnnotation label2 = EntityAnnotation.newBuilder()
                .setDescription("label2")
                .setScore(0.8f)
                .setTopicality(0.7f)
                .build();

        AnnotateImageResponse imageResponse = AnnotateImageResponse.newBuilder()
                .addLabelAnnotations(label1)
                .addLabelAnnotations(label2)
                .build();

        BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
                .addResponses(imageResponse)
                .build();

        when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);

        // When
        ImageAnalysisMetadata result = visionApiService.analyzeImageFromGcs(gcsUri);

        // Then
        assertNotNull(result.getOverallConfidence());
        assertEquals(0.85, result.getOverallConfidence(), 0.01);
    }

    @Test
    void analyzeImage_shouldProcessDominantColors() {
        // Given
        String gcsUri = "gs://test-bucket/image.jpg";
        Color color = Color.newBuilder()
                .setRed(255)
                .setGreen(128)
                .setBlue(64)
                .build();
        ColorInfo colorInfo = ColorInfo.newBuilder()
                .setColor(color)
                .setScore(0.5f)
                .setPixelFraction(0.3f)
                .build();
        DominantColorsAnnotation dominantColors = DominantColorsAnnotation.newBuilder()
                .addColors(colorInfo)
                .build();
        ImageProperties imageProperties = ImageProperties.newBuilder()
                .setDominantColors(dominantColors)
                .build();

        AnnotateImageResponse imageResponse = AnnotateImageResponse.newBuilder()
                .setImagePropertiesAnnotation(imageProperties)
                .build();

        BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
                .addResponses(imageResponse)
                .build();

        when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);

        // When
        ImageAnalysisMetadata result = visionApiService.analyzeImageFromGcs(gcsUri);

        // Then
        assertNotNull(result.getDominantColors());
        assertEquals(1, result.getDominantColors().size());
        assertEquals(255, result.getDominantColors().get(0).getRed());
        assertEquals(128, result.getDominantColors().get(0).getGreen());
        assertEquals(64, result.getDominantColors().get(0).getBlue());
        assertEquals(0.5, result.getDominantColors().get(0).getScore(), 0.01);
        assertEquals(0.3, result.getDominantColors().get(0).getPixelFraction(), 0.01);
    }

    @Test
    void analyzeImage_shouldProcessLogoAnnotation() {
        // Given
        String gcsUri = "gs://test-bucket/image.jpg";
        EntityAnnotation logo = EntityAnnotation.newBuilder()
                .setDescription("Coca-Cola")
                .setScore(0.92f)
                .build();

        AnnotateImageResponse imageResponse = AnnotateImageResponse.newBuilder()
                .addLogoAnnotations(logo)
                .build();

        BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
                .addResponses(imageResponse)
                .build();

        when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);

        // When
        ImageAnalysisMetadata result = visionApiService.analyzeImageFromGcs(gcsUri);

        // Then
        assertNotNull(result.getLogoAnnotation());
        assertEquals("Coca-Cola", result.getLogoAnnotation().getDescription());
        assertEquals(0.92, result.getLogoAnnotation().getScore(), 0.01);
    }

    @Test
    void analyzeImage_shouldThrowExceptionOnVisionApiError() {
        // Given
        String gcsUri = "gs://test-bucket/image.jpg";
        Status errorStatus = Status.newBuilder()
                .setCode(3)
                .setMessage("Invalid image format")
                .build();

        AnnotateImageResponse imageResponse = AnnotateImageResponse.newBuilder()
                .setError(errorStatus)
                .build();

        BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
                .addResponses(imageResponse)
                .build();

        when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);

        // When / Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            visionApiService.analyzeImageFromGcs(gcsUri);
        });
        assertTrue(exception.getMessage().contains("Invalid image format"));
    }

    @Test
    void analyzeImage_shouldRequestCorrectFeatures() {
        // Given
        String gcsUri = "gs://test-bucket/image.jpg";
        AnnotateImageResponse imageResponse = AnnotateImageResponse.newBuilder().build();

        BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
                .addResponses(imageResponse)
                .build();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AnnotateImageRequest>> requestCaptor = ArgumentCaptor.forClass(List.class);
        when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);

        // When
        visionApiService.analyzeImageFromGcs(gcsUri);

        // Then
        verify(visionClient).batchAnnotateImages(requestCaptor.capture());
        List<AnnotateImageRequest> requests = requestCaptor.getValue();
        assertEquals(1, requests.size());

        AnnotateImageRequest request = requests.get(0);
        assertEquals(5, request.getFeaturesCount());
    }

    @Test
    void extractTags_shouldReturnHighConfidenceLabels() {
        // Given
        ImageAnalysisMetadata metadata = ImageAnalysisMetadata.builder()
                .imageLabels(List.of(
                        ImageAnalysisMetadata.ImageLabel.builder()
                                .description("Bottle Cap")
                                .score(0.95)
                                .build(),
                        ImageAnalysisMetadata.ImageLabel.builder()
                                .description("Metal")
                                .score(0.85)
                                .build(),
                        ImageAnalysisMetadata.ImageLabel.builder()
                                .description("Low Confidence")
                                .score(0.5)
                                .build()
                ))
                .build();

        // When
        List<String> tags = visionApiService.extractTags(metadata);

        // Then
        assertEquals(2, tags.size());
        assertTrue(tags.contains("bottle cap"));
        assertTrue(tags.contains("metal"));
    }

    @Test
    void extractTags_shouldReturnEmptyListForNullMetadata() {
        // When
        List<String> tags = visionApiService.extractTags(null);

        // Then
        assertNotNull(tags);
        assertTrue(tags.isEmpty());
    }

    @Test
    void extractTags_shouldReturnEmptyListForNullLabels() {
        // Given
        ImageAnalysisMetadata metadata = ImageAnalysisMetadata.builder()
                .imageLabels(null)
                .build();

        // When
        List<String> tags = visionApiService.extractTags(metadata);

        // Then
        assertNotNull(tags);
        assertTrue(tags.isEmpty());
    }

    @Test
    void extractTags_shouldConvertToLowerCase() {
        // Given
        ImageAnalysisMetadata metadata = ImageAnalysisMetadata.builder()
                .imageLabels(List.of(
                        ImageAnalysisMetadata.ImageLabel.builder()
                                .description("UPPERCASE TAG")
                                .score(0.9)
                                .build()
                ))
                .build();

        // When
        List<String> tags = visionApiService.extractTags(metadata);

        // Then
        assertEquals(1, tags.size());
        assertEquals("uppercase tag", tags.get(0));
    }

    @Test
    void analyzeImage_shouldHandleEmptyResponse() {
        // Given
        String gcsUri = "gs://test-bucket/image.jpg";
        AnnotateImageResponse imageResponse = AnnotateImageResponse.newBuilder().build();

        BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
                .addResponses(imageResponse)
                .build();

        when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);

        // When
        ImageAnalysisMetadata result = visionApiService.analyzeImageFromGcs(gcsUri);

        // Then
        assertNotNull(result);
        assertNotNull(result.getImageLabels());
        assertTrue(result.getImageLabels().isEmpty());
        assertNull(result.getDominantColors());
        assertNull(result.getTextAnnotation());
        assertNull(result.getLogoAnnotation());
        assertNull(result.getOverallConfidence());
    }

}
