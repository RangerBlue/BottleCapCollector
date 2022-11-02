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
import com.google.api.client.util.Value;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import com.km.bottlecapcollector.exception.GoogleDriveException;
import com.km.bottlecapcollector.exception.ImageUploaderException;
import com.km.bottlecapcollector.property.AppProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;


@Component
@AllArgsConstructor
@Slf4j
public class GoogleDriveService implements ImageUploader{

    private static final String GRANT_TYPE = "refresh_token";
    private static final String AUTH_PROVIDER_X509_CERT_URL = "https://www.googleapis.com/oauth2/v1/certs";
    private static final String AUTH_URI = "https://accounts.google.com/o/oauth2/auth";

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);


    private final RestTemplate template = new RestTemplate();

    private final AppProperties appProperties;

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
        details.setClientId(appProperties.getGoogleDriveClientId());
        details.set("project_id", appProperties.getGoogleDriveProjectId());
        details.setAuthUri(AUTH_URI);
        details.set("auth_provider_x509_cert_url", AUTH_PROVIDER_X509_CERT_URL);
        details.setClientSecret(appProperties.getGoogleDriveClientSecret());
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

    @Override
    public String uploadFile(MultipartFile multipartFile) throws GoogleDriveException {
        log.info("Entering method uploadFile with " + multipartFile.getName()+ " file");
        ObjectMapper mapper = new ObjectMapper();
        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(getAccessToken());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Arrays.asList(new MediaType[]{MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_UTF8, MediaType.IMAGE_JPEG}));
        GoogleDriveItemMetadataRequest metadata = new GoogleDriveItemMetadataRequest();

        metadata.setMimeType(multipartFile.getContentType());
        metadata.setName(multipartFile.getOriginalFilename());
        metadata.setParents(new String[]{appProperties.getGoogleDriveFolderId()});

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

        ByteArrayResource contentsAsResource;

        try {
            contentsAsResource = new ByteArrayResource(multipartFile.getBytes()) {
                @Override
                public String getFilename() {
                    return multipartFile.getName();
                }

            };
        } catch (IOException e){
            throw new GoogleDriveException(e);
        }


        map.set("file", contentsAsResource);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<MultiValueMap<String, Object>>(map, headers);
        ResponseEntity<GoogleDriveItemResponse> response = template.postForEntity("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart", entity, GoogleDriveItemResponse.class);
        log.info("File " + multipartFile.getName() + " has been uploaded");

        return response.getBody().getId();
    }

    public String deleteFile(String fileID) throws ImageUploaderException {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());

        HttpEntity<String> entity = new HttpEntity(null, headers);
        String builder = "https://www.googleapis.com/drive/v3/files/" + fileID;
        ResponseEntity<String> response;
        try{
            response = template.exchange(builder, HttpMethod.DELETE, entity, String.class);
        }
        catch (RestClientException e){
            throw new ImageUploaderException(e);
        }

        return response.getBody();
    }

    @Override
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
