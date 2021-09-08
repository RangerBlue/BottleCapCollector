package com.km.bottlecapcollector.dto;

import lombok.Data;

@Data
public class BottleCapDto {
    private long id;
    private String fileLocation;
    private String googleDriveID;
    private String capName;
    private String description;
    private String creationDate;
    private int cols;
    private int rows;
    private String lastPreviewLinkUpdate;
    private double intersectionValue;
}
