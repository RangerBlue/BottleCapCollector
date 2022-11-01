package com.km.bottlecapcollector.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BottleCapCatalogDto extends BottleCapPictureDto {
    private String name;
    private String description;

}
