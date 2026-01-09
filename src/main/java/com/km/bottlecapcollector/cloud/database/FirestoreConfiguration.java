package com.km.bottlecapcollector.cloud.database;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.km.bottlecapcollector.property.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class FirestoreConfiguration {

    private final AppProperties appProperties;

    @Bean
    @Profile("gcp")
    public Firestore firestoreGcp() throws IOException {
        log.info("Initializing Firestore with Application Default Credentials (GCP profile)");

        FirestoreOptions.Builder builder = FirestoreOptions.newBuilder();

        String projectId = appProperties.getFirestoreProjectId();
        if (projectId != null && !projectId.isEmpty()) {
            builder.setProjectId(projectId);
            log.info("Using Firestore project ID: {}", projectId);
        }

        String databaseId = appProperties.getFirestoreDatabaseId();
        if (databaseId != null && !databaseId.isEmpty()) {
            builder.setDatabaseId(databaseId);
            log.info("Using Firestore database ID: {}", databaseId);
        }

        Firestore firestore = builder.build().getService();
        log.info("Firestore connection initialized successfully with ADC");
        return firestore;
    }

    @Bean
    @Profile("!gcp")
    public Firestore firestoreLocal() throws IOException {
        log.info("Initializing Firestore (local profile)");

        String emulatorHost = appProperties.getFirestoreEmulatorHost();
        String projectId = appProperties.getFirestoreProjectId();

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

        FirestoreOptions.Builder builder = FirestoreOptions.newBuilder();

        if (projectId != null && !projectId.isEmpty()) {
            builder.setProjectId(projectId);
            log.info("Using Firestore project ID: {}", projectId);
        }

        String databaseId = appProperties.getFirestoreDatabaseId();
        if (databaseId != null && !databaseId.isEmpty()) {
            builder.setDatabaseId(databaseId);
            log.info("Using Firestore database ID: {}", databaseId);
        }

        String credentialsPath = appProperties.getFirestoreCredentialsPath();
        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            log.info("Loading Firestore credentials from: {}", credentialsPath);
            builder.setCredentials(GoogleCredentials.fromStream(new FileInputStream(credentialsPath)));
        }

        Firestore firestore = builder.build().getService();
        log.info("Firestore connection initialized successfully");
        return firestore;
    }
}
