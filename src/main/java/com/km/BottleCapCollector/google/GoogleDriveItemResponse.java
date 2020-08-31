package com.km.BottleCapCollector.google;

public class GoogleDriveItemResponse {
    private String kind;
    private String id;
    private String name;
    private String mimeType;
    private String folderId;
    private String webViewLink;
    private String thumbnailLink;


    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public String getWebViewLink() {
        return webViewLink;
    }

    public void setWebViewLink(String webViewLink) {
        this.webViewLink = webViewLink;
    }

    public String getThumbnailLink() {
        return thumbnailLink.substring(0, thumbnailLink.lastIndexOf("="));
    }

    public void setThumbnailLink(String thumbnailLink) {
        this.thumbnailLink = thumbnailLink;
    }

    @Override
    public String toString() {
        return "GoogleDriveItemResponse{" +
                "kind='" + kind + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", folderId='" + folderId + '\'' +
                ", webViewLink='" + webViewLink + '\'' +
                ", thumbnailLink='" + getThumbnailLink() + '\'' +
                '}';
    }
}
