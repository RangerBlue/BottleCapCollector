package com.km.bottlecapcollector.api.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemIdentificationResponse {

    private String primaryName;

    private String brand;

    private String category;

    private String description;

    private List<String> suggestedTags;

    private Double confidence;
}
