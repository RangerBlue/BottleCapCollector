package com.km.bottlecapcollector.cloud.image.identification.gemini;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.Client;
import com.km.bottlecapcollector.property.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@Slf4j
public class GeminiConfiguration {

    private static final String DEFAULT_MODEL_ID = "gemini-2.0-flash";
    private static final String DEFAULT_LOCATION = "us-central1";

    private final AppProperties appProperties;

    public GeminiConfiguration(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public Client geminiClient() throws IOException {
        String projectId = appProperties.getVertexAiProjectId();
        String location = appProperties.getVertexAiLocation() != null
                ? appProperties.getVertexAiLocation() : DEFAULT_LOCATION;
        String credentialsPath = appProperties.getVertexAiCredentialsPath();

        if (projectId == null || projectId.isEmpty()) {
            log.warn("Vertex AI project ID not configured, Gemini service will be disabled");
            return null;
        }

        Client.Builder builder = Client.builder()
                .project(projectId)
                .location(location)
                .vertexAI(true);

        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            log.info("Using credentials file for Gemini: {}", credentialsPath);
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath))
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            builder.credentials(credentials);
        } else {
            log.info("Using application default credentials for Gemini");
        }

        Client client = builder.build();
        log.info("Initialized Gemini client with project: {}, location: {}", projectId, location);
        return client;
    }

    @Bean
    public String geminiModelId() {
        String modelId = appProperties.getVertexAiGeminiModelId() != null
                ? appProperties.getVertexAiGeminiModelId() : DEFAULT_MODEL_ID;
        log.info("Configured Gemini model: {}", modelId);
        return modelId;
    }
}
