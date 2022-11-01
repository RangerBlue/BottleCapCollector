package com.km.bottlecapcollector.dto;

import lombok.Data;

@Data
public class BottleCapDto {
    private long id;
    private String fileLocation;
    private String capName;
    private String description;
    private String creationDate;
}
