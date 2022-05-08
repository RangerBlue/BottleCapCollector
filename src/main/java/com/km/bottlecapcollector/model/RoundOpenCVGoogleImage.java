package com.km.bottlecapcollector.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@DiscriminatorValue("RoundImage")
@Entity
public class RoundOpenCVGoogleImage extends AbstractImage{

}
