package com.km.bottlecapcollector.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.*;
import java.io.IOException;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "Signature_Type")
@Data
public abstract class AbstractSignature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    public abstract void calculateParameters(MultipartFile file) throws IOException;
}
