package com.km.bottlecapcollector.model;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@DiscriminatorValue("RectangleImage")
@Entity
public class RectangleOpenCVGoogleImage extends AbstractImage{
}
