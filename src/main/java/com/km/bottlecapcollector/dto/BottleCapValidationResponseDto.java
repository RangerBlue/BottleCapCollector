package com.km.bottlecapcollector.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BottleCapValidationResponseDto {
    boolean isDuplicate;
    List<Long> similarCapsIDs;
    List<String> similarCapsURLs;
    int[] similarityDistribution;
}
