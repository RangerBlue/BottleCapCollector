package com.km.bottlecapcollector.model;

import com.km.bottlecapcollector.exception.ImageUploaderException;
import com.km.bottlecapcollector.storage.ImageUploader;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Entity
@DiscriminatorValue("S3")
@Data
public class AwsS3Provider extends AbstractImageProvider {
    @Override
    public String upload(MultipartFile file, ImageUploader uploader) throws ImageUploaderException {
        setImageProviderId(uploader.uploadFile(file));
        return uploader.getFileUrl(getImageProviderId());
    }
}
