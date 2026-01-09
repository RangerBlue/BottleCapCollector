package com.km.bottlecapcollector.cloud.database;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.auth.oauth2.GoogleCredentials;
import com.km.bottlecapcollector.property.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Firestore configuration: works with emulator locally and with ADC in Cloud Run.
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

        // --- DISABLE mTLS (fix for Cloud Run) ---
        System.setProperty("com.google.auth.mtls.enabled", "false");

        String emulatorHost = appProperties.getFirestoreEmulatorHost();
        String projectId = appProperties.getFirestoreProjectId();
        String databaseId = appProperties.getFirestoreDatabaseId();
        String credentialsPath = appProperties.getFirestoreCredentialsPath();

        // --- EMULATOR MODE ---
        if (emulatorHost != null && !emulatorHost.isEmpty()) {
            log.info("Connecting to Firestore emulator at: {}", emulatorHost);

            if (projectId == null || projectId.isEmpty()) {
                projectId = "demo-project";
            }

            FirestoreOptions options = FirestoreOptions.newBuilder()
                    .setProjectId(projectId)
                    .setEmulatorHost(emulatorHost)
                    .build();

            log.info("Firestore emulator connection initialized successfully");
            return options.getService();
        }

        // --- PRODUCTION MODE ---
        FirestoreOptions.Builder builder = FirestoreOptions.newBuilder();

        if (projectId != null && !projectId.isEmpty()) {
            builder.setProjectId(projectId);
            log.info("Using Firestore project ID: {}", projectId);
        }

        if (databaseId != null && !databaseId.isEmpty()) {
            builder.setDatabaseId(databaseId);
            log.info("Using Firestore database ID: {}", databaseId);
        }

        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            log.info("Loading Firestore credentials from: {}", credentialsPath);
            builder.setCredentials(GoogleCredentials.fromStream(new FileInputStream(credentialsPath)));
        } else {
            log.info("Using Application Default Credentials (ADC)");
            // Cloud Run automatically provides credentials via the attached service account
        }

        Firestore firestore = builder.build().getService();
        log.info("Firestore connection initialized successfully");
        return firestore;
    }
}
