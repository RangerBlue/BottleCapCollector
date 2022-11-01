package com.km.bottlecapcollector.model;

import com.km.bottlecapcollector.exception.ImageSignatureException;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "Signature_Type")
@Data
public abstract class AbstractSignature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    public abstract AbstractSignature calculateParameters(MultipartFile file) throws ImageSignatureException;
}
