package com.km.bottlecapcollector.property;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("bcc")
@Component
@Getter
@Setter
public class AppProperties {
    private String fileUploadDir;
    private String googleDriveClientId;
    private String googleDriveClientSecret;
    private String googleDriveProjectId;
    private String googleDriveFolderId;
    private String googleDriveTokenPath;
    private String googleDriveUpdateCron;
    private int validationResponseItemSize;
    private float similaritySearchRange;
    private int similaritySearchResponseSize;

    // Firestore configuration
    private String firestoreProjectId;
    private String firestoreDatabaseId;
    private String firestoreCredentialsPath;
    private String firestoreEmulatorHost;

    // Cloud Storage configuration
    private String cloudStorageBucketName;
    private String cloudStorageProjectId;
    private String cloudStorageCredentialsPath;

    // Vertex AI configuration
    private String vertexAiProjectId;
    private String vertexAiLocation;
    private String vertexAiEmbeddingModelId;
    private String vertexAiCredentialsPath;
    private String vertexAiGeminiModelId;

    // Vision API configuration
    private String visionApiProjectId;
    private String visionApiCredentialsPath;

    // Legacy API configuration
    private String legacyUserId;
    private String legacyCollectionKey;
    private String legacyCollectionName;

    // User limits
    private int maxItemsPerUser = 100;
}
