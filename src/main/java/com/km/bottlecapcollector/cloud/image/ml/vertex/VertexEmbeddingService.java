package com.km.bottlecapcollector.cloud.image.ml.vertex;

import com.google.cloud.aiplatform.v1.PredictResponse;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.km.bottlecapcollector.cloud.image.ml.api.Embedding;
import com.km.bottlecapcollector.cloud.image.ml.api.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
public class VertexEmbeddingService implements EmbeddingService {

    private final PredictionServiceClient predictionClient;
    private final String endpointName;
    private final String modelId;

    public VertexEmbeddingService(PredictionServiceClient predictionClient,
                                  @Qualifier("embeddingEndpointName") String endpointName,
                                  @Qualifier("embeddingModelId") String modelId) {
        this.predictionClient = predictionClient;
        this.endpointName = endpointName;
        this.modelId = modelId;
    }

    /**
     * Generates an embedding vector for an image file.
     *
     * @param file the image file
     * @return FirestoreEmbedding with the generated vector
     * @throws IOException if image processing fails
     */
    @Override
    public Embedding generateEmbedding(MultipartFile file){
        if (predictionClient == null) {
            log.warn("Embedding service not initialized, returning null");
            return null;
        }

        log.info("Generating embedding for image: {}", file.getOriginalFilename());

        byte[] imageBytes = null;
        try {
            imageBytes = file.getBytes();
        } catch (IOException e) {
            throw new VertexEmbeddingException(e.getMessage());
        }
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        String instanceJson = String.format("""
                {
                    "image": {
                        "bytesBase64Encoded": "%s"
                    }
                }
                """, base64Image);

        Value.Builder instanceBuilder = Value.newBuilder();
        try {
            JsonFormat.parser().merge(instanceJson, instanceBuilder);
        } catch (InvalidProtocolBufferException e) {
            throw new VertexEmbeddingException(e.getMessage());
        }
        Value instance = instanceBuilder.build();

        PredictResponse response = predictionClient.predict(endpointName, List.of(instance), Value.newBuilder().build());

        List<Float> embeddingVector = extractEmbeddingFromResponse(response);

        Embedding embedding = Embedding.builder()
                .vector(embeddingVector)
                .dimensions(embeddingVector.size())
                .modelId(modelId)
                .modelVersion("001")
                .generatedAt(Instant.now())
                .build();

        log.info("Generated embedding with {} dimensions", embeddingVector.size());
        return embedding;
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
    @Override
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
    @Override
    public boolean isAvailable() {
        return predictionClient != null;
    }
}
