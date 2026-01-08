package com.km.bottlecapcollector.cloud.storage.api;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    StorageImage uploadImage(MultipartFile file, String userId, String collectionKey);

    byte[] downloadImage(String objectName);

    boolean deleteImage(String objectName);

    String generateSignedUrl(String objectName);
}
