package com.km.bottlecapcollector.cloud.image.ml;

import com.google.cloud.aiplatform.v1.PredictResponse;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.km.bottlecapcollector.cloud.image.ml.api.Embedding;
import com.km.bottlecapcollector.cloud.image.ml.vertex.VertexEmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VertexEmbeddingServiceTests {

    private static final String ENDPOINT_NAME = "projects/test-project/locations/us-central1/publishers/google/models/multimodalembedding";
    private static final String MODEL_ID = "multimodalembedding@001";

    @Mock
    private PredictionServiceClient predictionClient;

    @Mock
    private MultipartFile multipartFile;

    private VertexEmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new VertexEmbeddingService(predictionClient, ENDPOINT_NAME, MODEL_ID);
    }

    @Test
    void generateEmbedding_shouldReturnEmbeddingWithVector() throws IOException {
        // Given
        byte[] imageBytes = "test image content".getBytes();
        when(multipartFile.getBytes()).thenReturn(imageBytes);
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");

        List<Double> embeddingValues = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5);
        PredictResponse response = createPredictResponse(embeddingValues);

        when(predictionClient.predict(eq(ENDPOINT_NAME), anyList(), any(Value.class))).thenReturn(response);

        // When
        Embedding result = embeddingService.generateEmbedding(multipartFile);

        // Then
        assertNotNull(result);
        assertNotNull(result.getVector());
        assertEquals(5, result.getVector().size());
        assertEquals(5, result.getDimensions());
        assertEquals(MODEL_ID, result.getModelId());
        assertEquals("001", result.getModelVersion());
        assertNotNull(result.getGeneratedAt());

        assertEquals(0.1f, result.getVector().get(0), 0.001);
        assertEquals(0.5f, result.getVector().get(4), 0.001);
    }

    @Test
    void generateEmbedding_shouldReturnNullWhenClientIsNull() throws IOException {
        // Given
        VertexEmbeddingService serviceWithNullClient = new VertexEmbeddingService(null, ENDPOINT_NAME, MODEL_ID);

        // When
        Embedding result = serviceWithNullClient.generateEmbedding(multipartFile);

        // Then
        assertNull(result);
    }

    @Test
    void generateEmbedding_shouldHandleEmptyResponse() throws IOException {
        // Given
        byte[] imageBytes = "test image content".getBytes();
        when(multipartFile.getBytes()).thenReturn(imageBytes);
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");

        PredictResponse emptyResponse = PredictResponse.newBuilder().build();
        when(predictionClient.predict(eq(ENDPOINT_NAME), anyList(), any(Value.class))).thenReturn(emptyResponse);

        // When
        Embedding result = embeddingService.generateEmbedding(multipartFile);

        // Then
        assertNotNull(result);
        assertNotNull(result.getVector());
        assertTrue(result.getVector().isEmpty());
        assertEquals(0, result.getDimensions());
    }

    @Test
    void calculateCosineSimilarity_shouldReturnOneForIdenticalVectors() {
        // Given
        List<Float> vector = Arrays.asList(1.0f, 2.0f, 3.0f);

        // When
        double similarity = embeddingService.calculateCosineSimilarity(vector, vector);

        // Then
        assertEquals(1.0, similarity, 0.0001);
    }

    @Test
    void calculateCosineSimilarity_shouldReturnCorrectValueForDifferentVectors() {
        // Given
        List<Float> vector1 = Arrays.asList(1.0f, 0.0f, 0.0f);
        List<Float> vector2 = Arrays.asList(0.0f, 1.0f, 0.0f);

        // When
        double similarity = embeddingService.calculateCosineSimilarity(vector1, vector2);

        // Then
        assertEquals(0.0, similarity, 0.0001);
    }

    @Test
    void calculateCosineSimilarity_shouldReturnCorrectValueForSimilarVectors() {
        // Given
        List<Float> vector1 = Arrays.asList(1.0f, 1.0f, 0.0f);
        List<Float> vector2 = Arrays.asList(1.0f, 0.0f, 0.0f);

        // When
        double similarity = embeddingService.calculateCosineSimilarity(vector1, vector2);

        // Then
        // cos(45°) ≈ 0.707
        assertEquals(0.707, similarity, 0.001);
    }

    @Test
    void calculateCosineSimilarity_shouldReturnZeroForNullFirstVector() {
        // Given
        List<Float> vector = Arrays.asList(1.0f, 2.0f, 3.0f);

        // When
        double similarity = embeddingService.calculateCosineSimilarity(null, vector);

        // Then
        assertEquals(0.0, similarity);
    }

    @Test
    void calculateCosineSimilarity_shouldReturnZeroForNullSecondVector() {
        // Given
        List<Float> vector = Arrays.asList(1.0f, 2.0f, 3.0f);

        // When
        double similarity = embeddingService.calculateCosineSimilarity(vector, null);

        // Then
        assertEquals(0.0, similarity);
    }

    @Test
    void calculateCosineSimilarity_shouldReturnZeroForEmptyFirstVector() {
        // Given
        List<Float> vector = Arrays.asList(1.0f, 2.0f, 3.0f);

        // When
        double similarity = embeddingService.calculateCosineSimilarity(Collections.emptyList(), vector);

        // Then
        assertEquals(0.0, similarity);
    }

    @Test
    void calculateCosineSimilarity_shouldReturnZeroForEmptySecondVector() {
        // Given
        List<Float> vector = Arrays.asList(1.0f, 2.0f, 3.0f);

        // When
        double similarity = embeddingService.calculateCosineSimilarity(vector, Collections.emptyList());

        // Then
        assertEquals(0.0, similarity);
    }

    @Test
    void calculateCosineSimilarity_shouldReturnZeroForDifferentSizedVectors() {
        // Given
        List<Float> vector1 = Arrays.asList(1.0f, 2.0f, 3.0f);
        List<Float> vector2 = Arrays.asList(1.0f, 2.0f);

        // When
        double similarity = embeddingService.calculateCosineSimilarity(vector1, vector2);

        // Then
        assertEquals(0.0, similarity);
    }

    @Test
    void calculateCosineSimilarity_shouldReturnZeroForZeroVector() {
        // Given
        List<Float> zeroVector = Arrays.asList(0.0f, 0.0f, 0.0f);
        List<Float> normalVector = Arrays.asList(1.0f, 2.0f, 3.0f);

        // When
        double similarity = embeddingService.calculateCosineSimilarity(zeroVector, normalVector);

        // Then
        assertEquals(0.0, similarity);
    }

    @Test
    void calculateCosineSimilarity_shouldHandleNegativeValues() {
        // Given
        List<Float> vector1 = Arrays.asList(1.0f, -1.0f, 0.0f);
        List<Float> vector2 = Arrays.asList(-1.0f, 1.0f, 0.0f);

        // When
        double similarity = embeddingService.calculateCosineSimilarity(vector1, vector2);

        // Then
        assertEquals(-1.0, similarity, 0.0001);
    }

    @Test
    void isAvailable_shouldReturnTrueWhenClientIsNotNull() {
        // When
        boolean available = embeddingService.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void isAvailable_shouldReturnFalseWhenClientIsNull() {
        // Given
        VertexEmbeddingService serviceWithNullClient = new VertexEmbeddingService(null, ENDPOINT_NAME, MODEL_ID);

        // When
        boolean available = serviceWithNullClient.isAvailable();

        // Then
        assertFalse(available);
    }

    private PredictResponse createPredictResponse(List<Double> embeddingValues) {
        ListValue.Builder listValueBuilder = ListValue.newBuilder();
        for (Double value : embeddingValues) {
            listValueBuilder.addValues(Value.newBuilder().setNumberValue(value).build());
        }

        Value imageEmbedding = Value.newBuilder()
                .setListValue(listValueBuilder.build())
                .build();

        Struct predictionStruct = Struct.newBuilder()
                .putFields("imageEmbedding", imageEmbedding)
                .build();

        Value prediction = Value.newBuilder()
                .setStructValue(predictionStruct)
                .build();

        return PredictResponse.newBuilder()
                .addPredictions(prediction)
                .build();
    }
}
