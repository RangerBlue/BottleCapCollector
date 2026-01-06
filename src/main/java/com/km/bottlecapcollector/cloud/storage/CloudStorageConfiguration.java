package com.km.bottlecapcollector.cloud.storage;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.km.bottlecapcollector.property.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Configuration class for Google Cloud Storage.
 */
@Configuration
@Slf4j
public class CloudStorageConfiguration {

    private final AppProperties appProperties;

    public CloudStorageConfiguration(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public Storage storage() throws IOException {
        log.info("Initializing Cloud Storage connection");

        StorageOptions.Builder optionsBuilder = StorageOptions.newBuilder();

        String projectId = appProperties.getCloudStorageProjectId();
        if (projectId != null && !projectId.isEmpty()) {
            optionsBuilder.setProjectId(projectId);
            log.info("Using Cloud Storage project ID: {}", projectId);
        }

        // Check for emulator (for local development)
        String emulatorHost = System.getenv("STORAGE_EMULATOR_HOST");
        if (emulatorHost != null && !emulatorHost.isEmpty()) {
            log.info("Using Cloud Storage emulator at: {}", emulatorHost);
            optionsBuilder.setHost(emulatorHost);
        }

        String credentialsPath = appProperties.getCloudStorageCredentialsPath();
        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            log.info("Loading Cloud Storage credentials from: {}", credentialsPath);
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new FileInputStream(credentialsPath));
            optionsBuilder.setCredentials(credentials);
        } else {
            log.info("Using default Cloud Storage credentials (Application Default Credentials)");
        }

        Storage storage = optionsBuilder.build().getService();
        log.info("Cloud Storage connection initialized successfully");
        return storage;
    }
}
