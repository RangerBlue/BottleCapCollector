package com.km.bottlecapcollector.dto;

import lombok.Data;

@Data
public class BottleCapDto extends CapPictureDto{
    private long id;
    private String name;
    private String description;
    private String creationDate;
}
