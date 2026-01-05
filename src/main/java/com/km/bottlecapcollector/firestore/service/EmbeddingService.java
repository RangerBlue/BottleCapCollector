package com.km.bottlecapcollector.firestore.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.aiplatform.v1.EndpointName;
import com.google.cloud.aiplatform.v1.PredictResponse;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.km.bottlecapcollector.firestore.document.FirestoreEmbedding;
import com.km.bottlecapcollector.property.AppProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Service for generating image embeddings using Vertex AI Multimodal Embeddings.
 * Uses the multimodalembedding model to generate 1408-dimensional vectors for images.
 */
@Service
@Slf4j
public class EmbeddingService {

    private static final String DEFAULT_MODEL_ID = "multimodalembedding@001";
    private static final int EMBEDDING_DIMENSIONS = 1408;

    private final AppProperties appProperties;
    private PredictionServiceClient predictionClient;
    private String endpointName;

    public EmbeddingService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() throws IOException {
        String projectId = appProperties.getVertexAiProjectId();
        String location = appProperties.getVertexAiLocation() != null
                ? appProperties.getVertexAiLocation() : "us-central1";
        String modelId = appProperties.getVertexAiEmbeddingModelId() != null
                ? appProperties.getVertexAiEmbeddingModelId() : DEFAULT_MODEL_ID;
        String credentialsPath = appProperties.getVertexAiCredentialsPath();

        if (projectId == null || projectId.isEmpty()) {
            log.warn("Vertex AI project ID not configured, embedding service will be disabled");
            return;
        }

        String endpoint = String.format("%s-aiplatform.googleapis.com:443", location);

        PredictionServiceSettings.Builder settingsBuilder = PredictionServiceSettings.newBuilder()
                .setEndpoint(endpoint);

        // Use credentials file if specified, otherwise use default credentials
        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            log.info("Using credentials file for Vertex AI: {}", credentialsPath);
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath))
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            settingsBuilder.setCredentialsProvider(() -> credentials);
        } else {
            log.info("Using application default credentials for Vertex AI");
        }

        this.predictionClient = PredictionServiceClient.create(settingsBuilder.build());
        this.endpointName = EndpointName.ofProjectLocationPublisherModelName(
                projectId, location, "google", modelId).toString();

        log.info("Initialized EmbeddingService with endpoint: {}", endpointName);
    }

    @PreDestroy
    public void cleanup() {
        if (predictionClient != null) {
            predictionClient.close();
        }
    }

    /**
     * Generates an embedding vector for an image file.
     *
     * @param file the image file
     * @return FirestoreEmbedding with the generated vector
     * @throws IOException if image processing fails
     */
    public FirestoreEmbedding generateEmbedding(MultipartFile file) throws IOException {
        if (predictionClient == null) {
            log.warn("Embedding service not initialized, returning null");
            return null;
        }

        log.info("Generating embedding for image: {}", file.getOriginalFilename());

        // Encode image to base64
        byte[] imageBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Build the request
        String instanceJson = String.format("""
                {
                    "image": {
                        "bytesBase64Encoded": "%s"
                    }
                }
                """, base64Image);

        Value.Builder instanceBuilder = Value.newBuilder();
        JsonFormat.parser().merge(instanceJson, instanceBuilder);
        Value instance = instanceBuilder.build();

        // Call the prediction API
        PredictResponse response = predictionClient.predict(endpointName, List.of(instance), Value.newBuilder().build());

        // Extract embedding from response
        List<Float> embeddingVector = extractEmbeddingFromResponse(response);

        String modelId = appProperties.getVertexAiEmbeddingModelId() != null
                ? appProperties.getVertexAiEmbeddingModelId() : DEFAULT_MODEL_ID;

        FirestoreEmbedding embedding = FirestoreEmbedding.builder()
                .vector(embeddingVector)
                .dimensions(embeddingVector.size())
                .modelId(modelId)
                .modelVersion("001")
                .generatedAt(Instant.now())
                .build();

        log.info("Generated embedding with {} dimensions", embeddingVector.size());
        return embedding;
    }

    /**
     * Generates an embedding vector from raw image bytes.
     *
     * @param imageBytes the image bytes
     * @return FirestoreEmbedding with the generated vector
     */
    public FirestoreEmbedding generateEmbedding(byte[] imageBytes) {
        if (predictionClient == null) {
            log.warn("Embedding service not initialized, returning null");
            return null;
        }

        log.info("Generating embedding from image bytes");

        // Encode image to base64
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Build the request
        String instanceJson = String.format("""
                {
                    "image": {
                        "bytesBase64Encoded": "%s"
                    }
                }
                """, base64Image);

        try {
            Value.Builder instanceBuilder = Value.newBuilder();
            JsonFormat.parser().merge(instanceJson, instanceBuilder);
            Value instance = instanceBuilder.build();

            // Call the prediction API
            PredictResponse response = predictionClient.predict(endpointName, List.of(instance), Value.newBuilder().build());

            // Extract embedding from response
            List<Float> embeddingVector = extractEmbeddingFromResponse(response);

            String modelId = appProperties.getVertexAiEmbeddingModelId() != null
                    ? appProperties.getVertexAiEmbeddingModelId() : DEFAULT_MODEL_ID;

            FirestoreEmbedding embedding = FirestoreEmbedding.builder()
                    .vector(embeddingVector)
                    .dimensions(embeddingVector.size())
                    .modelId(modelId)
                    .modelVersion("001")
                    .generatedAt(Instant.now())
                    .build();

            log.info("Generated embedding with {} dimensions", embeddingVector.size());
            return embedding;

        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    private List<Float> extractEmbeddingFromResponse(PredictResponse response) {
        List<Float> embedding = new ArrayList<>();

        if (response.getPredictionsCount() > 0) {
            Value prediction = response.getPredictions(0);
            Value imageEmbedding = prediction.getStructValue().getFieldsMap().get("imageEmbedding");

            if (imageEmbedding != null) {
                for (Value value : imageEmbedding.getListValue().getValuesList()) {
                    embedding.add((float) value.getNumberValue());
                }
            }
        }

        if (embedding.isEmpty()) {
            log.warn("No embedding found in response");
        }

        return embedding;
    }

    /**
     * Calculates cosine similarity between two embedding vectors.
     *
     * @param embedding1 first embedding vector
     * @param embedding2 second embedding vector
     * @return similarity score between 0.0 and 1.0 (1.0 = identical)
     */
    public double calculateCosineSimilarity(List<Float> embedding1, List<Float> embedding2) {
        if (embedding1 == null || embedding2 == null ||
            embedding1.isEmpty() || embedding2.isEmpty() ||
            embedding1.size() != embedding2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < embedding1.size(); i++) {
            double v1 = embedding1.get(i);
            double v2 = embedding2.get(i);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Checks if the embedding service is available.
     *
     * @return true if the service is initialized and ready
     */
    public boolean isAvailable() {
        return predictionClient != null;
    }
}
