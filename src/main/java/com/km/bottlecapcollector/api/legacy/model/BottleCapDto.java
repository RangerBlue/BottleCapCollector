package com.km.bottlecapcollector.api.legacy.model;

import lombok.Data;

@Data
public class BottleCapDto extends CapPictureDto {
    private String name;
    private String description;
    private String creationDate;
}
