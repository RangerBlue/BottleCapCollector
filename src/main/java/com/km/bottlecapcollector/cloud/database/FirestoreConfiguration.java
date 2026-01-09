package com.km.bottlecapcollector.cloud.database;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.km.bottlecapcollector.property.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Firestore configuration: works locally with emulator, and on Cloud Run with ADC
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
        String projectId = appProperties.getFirestoreProjectId();
        String databaseId = appProperties.getFirestoreDatabaseId();
        String credentialsPath = appProperties.getFirestoreCredentialsPath();

        // --- Emulator mode ---
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

        // --- Production mode (Cloud Run) ---
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
            builder.setCredentials(
                    com.google.auth.oauth2.GoogleCredentials.fromStream(new FileInputStream(credentialsPath))
            );
        } else {
            log.info("Using Application Default Credentials");
            // Cloud Run automatically provides credentials via attached service account
        }

        // --- FORCE TLS (disable mTLS) ---
        // Latest SDK uses gRPC; we can prevent mTLS by forcing JDK TLS
        System.setProperty("com.google.auth.mtls.enabled", "false");

        Firestore firestore = builder.build().getService();
        log.info("Firestore connection initialized successfully");
        return firestore;
    }
}