package com.km.bottlecapcollector.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@DiscriminatorValue("S3RoundImage")
@Entity
public class RoundOpenCVS3Image extends AbstractImage{
}
