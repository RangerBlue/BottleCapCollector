package com.km.bottlecapcollector.google;

import com.km.bottlecapcollector.api.legacy.exception.ImageUploaderException;
import org.springframework.web.multipart.MultipartFile;


public interface ImageUploader {
    String uploadFile(MultipartFile multipartFile) throws ImageUploaderException;

    String getFileUrl(String id);

    String deleteFile(String fileID) throws ImageUploaderException;
}
