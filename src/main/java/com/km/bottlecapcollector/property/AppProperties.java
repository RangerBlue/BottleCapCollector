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
    private String imageProvider;
}
