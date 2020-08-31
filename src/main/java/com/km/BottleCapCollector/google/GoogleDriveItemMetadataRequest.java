package com.km.BottleCapCollector.google;

public class GoogleDriveItemMetadataRequest {
    public String name;
    public String mimeType;
    public String[] parents;

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

    public String[] getParents() {
        return parents;
    }

    public void setParents(String[] parents) {
        this.parents = parents;
    }
}
