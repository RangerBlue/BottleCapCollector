package com.km.bottlecapcollector.api.legacy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BottleCapValidationResponseDto {
    boolean isDuplicate;
    List<String> similarCapsIDs;
    List<String> similarCapsURLs;
    int[] similarityDistribution;
}
