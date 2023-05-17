package com.km.bottlecapcollector.model;

import com.km.bottlecapcollector.exception.ImageUploaderException;
import com.km.bottlecapcollector.google.ImageUploader;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Entity
@DiscriminatorValue("GoogleDrive")
@Data
public class GoogleDriveProvider extends AbstractImageProvider {
    @Override
    public String upload(MultipartFile file, ImageUploader uploader) throws ImageUploaderException {
        setImageProviderId(uploader.uploadFile(file));
        return uploader.getFileUrl(getImageProviderId());
    }
}
