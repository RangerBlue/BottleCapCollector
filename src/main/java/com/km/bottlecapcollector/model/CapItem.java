package com.km.bottlecapcollector.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;

@Entity
@DiscriminatorValue("Cap")
@Data
public class CapItem extends CollectionItem{
    private String name;
    private String description;
}
