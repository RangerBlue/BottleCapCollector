package com.km.bottlecapcollector.cloud.image.ml.api;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface EmbeddingService {
    Embedding generateEmbedding(MultipartFile file) throws IOException;

    double calculateCosineSimilarity(List<Float> embedding1, List<Float> embedding2);

    boolean isAvailable();
}
