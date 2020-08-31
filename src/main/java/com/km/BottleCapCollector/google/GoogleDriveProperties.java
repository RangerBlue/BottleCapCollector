package com.km.BottleCapCollector.google;

import com.google.api.client.util.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class GoogleDriveProperties {
    @Value("${CLIENT_ID}")
    private String clientID;

    @Value("${PROJECT_ID}")
    private String projectID;

    @Value("${AUTH_URI}")
    private String authUri;

    @Value("${TOKEN_URI}")
    private String tokenUri;

    @Value("${AUTH_PROVIDER_X509_CERT_URL}")
    private String authProviderX509CertUrl;

    @Value("${CLIENT_SECRET}")
    private String clientSecret;

    @Value("${FOLDER_ID}")
    private String folderID;

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getProjectID() {
        return projectID;
    }

    public void setProjectID(String projectID) {
        this.projectID = projectID;
    }

    public String getAuthUri() {
        return authUri;
    }

    public void setAuthUri(String authUri) {
        this.authUri = authUri;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public void setTokenUri(String tokenUri) {
        this.tokenUri = tokenUri;
    }

    public String getAuthProviderX509CertUrl() {
        return authProviderX509CertUrl;
    }

    public void setAuthProviderX509CertUrl(String authProviderX509CertUrl) {
        this.authProviderX509CertUrl = authProviderX509CertUrl;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getFolderID() {
        return folderID;
    }

    public void setFolderID(String folderID) {
        this.folderID = folderID;
    }
}
