package com.km.BottleCapCollector.property;


@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "file")
public class CustomProperties {
    private String uploadDir;

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }
}
