package com.km.bottlecapcollector.cloud.image.identification.gemini;

import com.km.bottlecapcollector.cloud.image.identification.api.ImageIdentification;
import org.springframework.web.multipart.MultipartFile;

public interface IdentificationService {
    boolean isAvailable();

    ImageIdentification identifyItem(MultipartFile file);
}
