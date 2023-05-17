package com.km.bottlecapcollector.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@DiscriminatorValue("RoundImage")
@Entity
public class RoundOpenCVGoogleImage extends AbstractImage{

}
