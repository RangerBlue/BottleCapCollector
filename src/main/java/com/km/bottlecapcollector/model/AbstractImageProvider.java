package com.km.bottlecapcollector.model;

import com.km.bottlecapcollector.exception.ImageUploaderException;
import com.km.bottlecapcollector.google.ImageUploader;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Data
public abstract class AbstractImageProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String imageProviderId;

    private String fileName;

    @UpdateTimestamp
    private LocalDateTime updateDateTime;

    public abstract String upload(MultipartFile file, ImageUploader uploader) throws ImageUploaderException;
}
