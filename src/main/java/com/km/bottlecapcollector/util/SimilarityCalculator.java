package com.km.bottlecapcollector.util;

import org.springframework.web.multipart.MultipartFile;

public interface SimilarityCalculator {
     SimilarityModel calculateSimilarityModel(String itemName, MultipartFile file, int resultSize);
}
