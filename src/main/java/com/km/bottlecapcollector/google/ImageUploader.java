package com.km.bottlecapcollector.google;

import com.km.bottlecapcollector.exception.GoogleDriveException;
import com.km.bottlecapcollector.exception.ImageUploaderException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ImageUploader {
    String uploadFile(MultipartFile multipartFile) throws ImageUploaderException;

    String getFileUrl(String id);

    String deleteFile(String fileID) throws ImageUploaderException;
}
