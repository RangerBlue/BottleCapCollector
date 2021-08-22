package com.km.bottlecapcollector.google;

public class GoogleDriveUploadItem {
    private String contentType;
    private String originalFilename;
    private byte[] byteArray;
    private String fileName;

    public GoogleDriveUploadItem(String contentType, String originalFilename, String fileName, byte[] byteArray) {
        this.contentType = contentType;
        this.originalFilename = originalFilename;
        this.fileName = fileName;
        this.byteArray = byteArray;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public byte[] getByteArray() {
        return byteArray;
    }

    public void setByteArray(byte[] byteArray) {
        this.byteArray = byteArray;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

}
