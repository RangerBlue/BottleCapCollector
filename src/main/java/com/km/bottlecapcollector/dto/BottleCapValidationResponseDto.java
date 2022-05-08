package com.km.bottlecapcollector.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BottleCapValidationResponseDto {
    boolean isDuplicate;
    List<Long> similarCapsIDs;
    List<String> similarCapsURLs;
    int[] similarityDistribution;
}
