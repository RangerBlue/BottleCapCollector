package com.km.bottlecapcollector.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;

@Entity
@DiscriminatorValue("Picture")
@Data
public class PictureItem extends CollectionItem {
    private String name;
    private String description;
}
