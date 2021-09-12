package com.km.bottlecapcollector.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BottleCapCatalogDto extends BottleCapPictureDto {
    private String name;
    private String description;

    public BottleCapCatalogDto(long id, String url, String name, String description) {
        super(id, url);
        this.name = name;
        this.description = description;
    }
}
