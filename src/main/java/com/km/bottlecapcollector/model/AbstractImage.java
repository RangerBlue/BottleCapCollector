package com.km.bottlecapcollector.model;

import lombok.Data;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;

@Entity(name = "ABSTRACTIMAGE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "IMAGE_TYPE")
@Data
public abstract class AbstractImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String url;
    private float hue;
    private float saturation;
    private float brightness;

    @OneToOne
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    @JoinColumn(name = "singature_id", referencedColumnName = "id")
    private AbstractSignature signature;

    @OneToOne
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    @JoinColumn(name = "provider_id", referencedColumnName = "id")
    private AbstractImageProvider provider;
}
