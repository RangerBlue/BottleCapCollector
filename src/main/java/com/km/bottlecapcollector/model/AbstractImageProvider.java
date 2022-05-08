package com.km.bottlecapcollector.model;

import com.km.bottlecapcollector.google.ImageUploader;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.*;
import java.io.IOException;
import java.time.LocalDateTime;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public abstract class AbstractImageProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String imageProviderId;

    @UpdateTimestamp
    private LocalDateTime updateDateTime;

    public abstract String upload(MultipartFile file, ImageUploader uploader) throws IOException;
}
