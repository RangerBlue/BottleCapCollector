package com.km.BottleCapCollector.property;


@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "file")
public class CustomProperties {
    private String uploadDir;
    private String objectDir;

    public void setObjectDir(String objectDir) {
        this.objectDir = objectDir;
    }

    public String getObjectDir() {
        return objectDir;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }
}
