package com.km.bottlecapcollector.gcp.storage.api;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {
    StorageImage uploadImage(MultipartFile file, String bottleCapId) throws IOException;

    byte[] downloadImage(String objectName);

    boolean deleteImage(String objectName);

    String generateSignedUrl(String objectName, int durationMinutes);
}
