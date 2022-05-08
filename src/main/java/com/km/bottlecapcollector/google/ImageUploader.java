package com.km.bottlecapcollector.google;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ImageUploader {
    String uploadFile(MultipartFile multipartFile) throws IOException;

    String getFileUrl(String id);
}
