package com.km.bottlecapcollector.dto;

public class CapWrapper extends PictureWrapper{
    private String name;
    private String description;

    public CapWrapper(long id, String url, String name, String description) {
        super(id, url);
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
