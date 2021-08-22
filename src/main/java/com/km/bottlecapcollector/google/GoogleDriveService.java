package com.km.bottlecapcollector.google;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.*;


@Component
public class GoogleDriveService {

    private static final Logger logger = LogManager.getLogger(GoogleDriveService.class);

    @Autowired
    GoogleDriveProperties properties;

    /**
     * Application name.
     */
    private static final String APPLICATION_NAME = "BottleCapCollector";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);

    private RestTemplate template = new RestTemplate();

    public String getFolderID() {
        return properties.getFolderID();
    }

    public String getAccessToken() {
        String token = "";
        try {
            NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = getCredentials(HTTP_TRANSPORT);
            credential.refreshToken();
            token = credential.getAccessToken();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return token;
    }

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    public Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
        details.setClientId(properties.getClientID());
        details.set("project_id", properties.getProjectID());
        details.setAuthUri(properties.getAuthUri());
        details.set("auth_provider_x509_cert_url", properties.getAuthProviderX509CertUrl());
        details.setClientSecret(properties.getClientSecret());
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
        clientSecrets.setInstalled(details);

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public String uploadFile(GoogleDriveUploadItem googleUploadItemDto) {
        logger.info("Entering method uploadFile with " + googleUploadItemDto.getFileName() + " file");
        ObjectMapper mapper = new ObjectMapper();
        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(getAccessToken());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Arrays.asList(new MediaType[]{MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_UTF8, MediaType.IMAGE_JPEG}));
        GoogleDriveItemMetadataRequest metadata = new GoogleDriveItemMetadataRequest();

        metadata.setMimeType(googleUploadItemDto.getContentType());
        metadata.setName(googleUploadItemDto.getOriginalFilename());
        metadata.setParents(new String[]{getFolderID()});

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();

        HttpHeaders metadataHeaders = new HttpHeaders();
        metadataHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> metadataEntity = null;
        try {
            metadataEntity = new HttpEntity<>(mapper.writeValueAsString(metadata), metadataHeaders);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        map.set("metadata", metadataEntity);
        ByteArrayResource contentsAsResource = new ByteArrayResource(googleUploadItemDto.getByteArray()) {
            @Override
            public String getFilename() {
                return googleUploadItemDto.getFileName();
            }

        };

        map.set("file", contentsAsResource);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<MultiValueMap<String, Object>>(map, headers);
        ResponseEntity<GoogleDriveItemResponse> response = template.postForEntity("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart", entity, GoogleDriveItemResponse.class);
        logger.info("File " + googleUploadItemDto.getFileName() + " has been uploaded");

        return response.getBody().getId();
    }

    public String deleteFile(String fileID) throws HttpClientErrorException.NotFound {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());

        HttpEntity<String> entity = new HttpEntity(null, headers);
        String builder = "https://www.googleapis.com/drive/v3/files/" + fileID;
        ResponseEntity<String> response = template.exchange(builder, HttpMethod.DELETE, entity, String.class);
        return response.getBody();
    }

    public String getFileUrl(String id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());

        HttpEntity<String> entity = new HttpEntity(null, headers);
        String builder = "https://www.googleapis.com/drive/v3/files/" +
                id +
                "?fields=*";
        ResponseEntity<GoogleDriveItemResponse> response = template.exchange(builder, HttpMethod.GET, entity, GoogleDriveItemResponse.class);
        return response.getBody().getThumbnailLink();

    }
}
