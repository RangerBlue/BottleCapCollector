package com.km.bottlecapcollector.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@DiscriminatorValue("RectangleImage")
@Entity
public class RectangleOpenCVGoogleImage extends AbstractImage{
}
