package com.km.bottlecapcollector.firestore.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.km.bottlecapcollector.property.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Configuration class for Google Cloud Firestore.
 */
@Configuration
@Slf4j
public class FirestoreConfiguration {

    private final AppProperties appProperties;

    public FirestoreConfiguration(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public Firestore firestore() throws IOException {
        log.info("Initializing Firestore connection");

        String emulatorHost = appProperties.getFirestoreEmulatorHost();

        // Check if emulator host is configured
        if (emulatorHost != null && !emulatorHost.isEmpty()) {
            log.info("Connecting to Firestore emulator at: {}", emulatorHost);

            String projectId = appProperties.getFirestoreProjectId();
            if (projectId == null || projectId.isEmpty()) {
                projectId = "demo-project";
            }

            FirestoreOptions options = FirestoreOptions.newBuilder()
                    .setProjectId(projectId)
                    .setEmulatorHost(emulatorHost)
                    .build();

            Firestore firestore = options.getService();
            log.info("Firestore emulator connection initialized successfully");
            return firestore;
        }

        // Production configuration
        FirestoreOptions.Builder optionsBuilder = FirestoreOptions.newBuilder();

        String projectId = appProperties.getFirestoreProjectId();
        if (projectId != null && !projectId.isEmpty()) {
            optionsBuilder.setProjectId(projectId);
            log.info("Using Firestore project ID: {}", projectId);
        }

        String databaseId = appProperties.getFirestoreDatabaseId();
        if (databaseId != null && !databaseId.isEmpty()) {
            optionsBuilder.setDatabaseId(databaseId);
            log.info("Using Firestore database ID: {}", databaseId);
        }

        String credentialsPath = appProperties.getFirestoreCredentialsPath();
        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            log.info("Loading Firestore credentials from: {}", credentialsPath);
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new FileInputStream(credentialsPath));
            optionsBuilder.setCredentials(credentials);
        } else {
            log.info("Using default Firestore credentials (Application Default Credentials)");
        }

        FirestoreOptions options = optionsBuilder.build();
        Firestore firestore = options.getService();

        log.info("Firestore connection initialized successfully");
        return firestore;
    }
}
