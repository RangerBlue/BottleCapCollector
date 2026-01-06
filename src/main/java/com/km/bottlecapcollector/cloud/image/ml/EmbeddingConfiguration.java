package com.km.bottlecapcollector.cloud.image.ml;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.aiplatform.v1.EndpointName;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import com.km.bottlecapcollector.property.AppProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@Slf4j
public class EmbeddingConfiguration {

    private static final String DEFAULT_MODEL_ID = "multimodalembedding@001";
    private static final String DEFAULT_LOCATION = "us-central1";

    private final AppProperties appProperties;
    private PredictionServiceClient predictionClient;

    public EmbeddingConfiguration(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public PredictionServiceClient predictionServiceClient() throws IOException {
        String projectId = appProperties.getVertexAiProjectId();
        String location = appProperties.getVertexAiLocation() != null
                ? appProperties.getVertexAiLocation() : DEFAULT_LOCATION;
        String credentialsPath = appProperties.getVertexAiCredentialsPath();

        if (projectId == null || projectId.isEmpty()) {
            log.warn("Vertex AI project ID not configured, embedding service will be disabled");
            return null;
        }

        String endpoint = String.format("%s-aiplatform.googleapis.com:443", location);

        PredictionServiceSettings.Builder settingsBuilder = PredictionServiceSettings.newBuilder()
                .setEndpoint(endpoint);

        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            log.info("Using credentials file for Vertex AI: {}", credentialsPath);
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath))
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            settingsBuilder.setCredentialsProvider(() -> credentials);
        } else {
            log.info("Using application default credentials for Vertex AI");
        }

        this.predictionClient = PredictionServiceClient.create(settingsBuilder.build());
        log.info("Initialized PredictionServiceClient with endpoint: {}", endpoint);
        return this.predictionClient;
    }

    @Bean
    public String embeddingEndpointName() {
        String projectId = appProperties.getVertexAiProjectId();
        String location = appProperties.getVertexAiLocation() != null
                ? appProperties.getVertexAiLocation() : DEFAULT_LOCATION;
        String modelId = appProperties.getVertexAiEmbeddingModelId() != null
                ? appProperties.getVertexAiEmbeddingModelId() : DEFAULT_MODEL_ID;

        if (projectId == null || projectId.isEmpty()) {
            return null;
        }

        String endpointName = EndpointName.ofProjectLocationPublisherModelName(
                projectId, location, "google", modelId).toString();
        log.info("Configured embedding endpoint: {}", endpointName);
        return endpointName;
    }

    @Bean
    public String embeddingModelId() {
        return appProperties.getVertexAiEmbeddingModelId() != null
                ? appProperties.getVertexAiEmbeddingModelId() : DEFAULT_MODEL_ID;
    }

    @PreDestroy
    public void cleanup() {
        if (predictionClient != null) {
            predictionClient.close();
            log.info("Closed PredictionServiceClient");
        }
    }
}
