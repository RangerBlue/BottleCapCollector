package com.km.bottlecapcollector.model.enums;

import com.km.bottlecapcollector.model.CollectionItem;
import lombok.Data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("Picture")
@Data
public class PictureItem extends CollectionItem {
    private String name;
    private String description;
}
