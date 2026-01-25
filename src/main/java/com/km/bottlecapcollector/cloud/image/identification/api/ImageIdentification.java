package com.km.bottlecapcollector.cloud.image.identification.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageIdentification {

    private String primaryName;

    private String brand;

    private String category;

    private String description;

    private List<String> suggestedTags;

    private Double confidence;

    private String rawResponse;
}
