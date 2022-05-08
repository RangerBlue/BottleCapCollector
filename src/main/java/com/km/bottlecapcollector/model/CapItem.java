package com.km.bottlecapcollector.model;

import lombok.Data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("Cap")
@Data
public class CapItem extends CollectionItem{
    private String name;
    private String description;
}
