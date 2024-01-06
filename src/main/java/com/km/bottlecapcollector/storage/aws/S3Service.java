package com.km.bottlecapcollector.storage.aws;

import com.km.bottlecapcollector.exception.ImageUploaderException;
import com.km.bottlecapcollector.storage.ImageUploader;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


import java.io.IOException;

@Service
@AllArgsConstructor
@ConditionalOnProperty(value = "bcc.image.provider", havingValue = "s3")
public class S3Service implements ImageUploader {

    private final S3Config s3Config;

    private final S3Client s3client;

    @Override
    public String uploadFile(MultipartFile multipartFile) throws ImageUploaderException {
        try {
            var putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucket())
                    .key(multipartFile.getOriginalFilename())
                    .build();
            s3client.putObject(putObjectRequest, RequestBody.fromBytes(multipartFile.getBytes()));
        } catch (IOException e) {
            throw new ImageUploaderException(e);
        }

        return multipartFile.getOriginalFilename();
    }

    @Override
    public String getFileUrl(String id) {
        return s3client.utilities().getUrl(builder -> builder.bucket(s3Config.getBucket()).key(id)).toExternalForm();
    }

    @Override
    public String deleteFile(String fileID) throws ImageUploaderException {
        return null;
    }
}
