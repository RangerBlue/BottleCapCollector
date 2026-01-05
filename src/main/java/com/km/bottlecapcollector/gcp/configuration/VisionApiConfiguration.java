package com.km.bottlecapcollector.gcp.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.km.bottlecapcollector.property.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Configuration class for Google Cloud Vision API.
 */
@Configuration
@Slf4j
public class VisionApiConfiguration {

    private final AppProperties appProperties;

    public VisionApiConfiguration(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public ImageAnnotatorClient imageAnnotatorClient() throws IOException {
        log.info("Initializing Vision API client");

        String credentialsPath = appProperties.getVisionApiCredentialsPath();

        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            log.info("Loading Vision API credentials from: {}", credentialsPath);
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new FileInputStream(credentialsPath));

            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build();

            ImageAnnotatorClient client = ImageAnnotatorClient.create(settings);
            log.info("Vision API client initialized with custom credentials");
            return client;
        }

        // Use Application Default Credentials
        log.info("Using default Vision API credentials (Application Default Credentials)");
        ImageAnnotatorClient client = ImageAnnotatorClient.create();
        log.info("Vision API client initialized successfully");
        return client;
    }
}
