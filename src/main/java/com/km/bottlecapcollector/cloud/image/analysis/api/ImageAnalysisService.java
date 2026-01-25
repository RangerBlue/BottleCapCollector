package com.km.bottlecapcollector.cloud.image.analysis.api;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageAnalysisService {
    ImageAnalysisMetadata analyzeImageFromGcs(String gcsUri);

    ImageAnalysisMetadata analyzeImageFromFile(MultipartFile file);

    ImageAnalysisMetadata analyzeImageFromBytes(byte[] imageBytes);

    List<String> extractTags(ImageAnalysisMetadata metadata);
}
