package com.km.bottlecapcollector.api.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "AI-powered item identification response")
public class ItemIdentificationResponse {

    @Schema(description = "Primary name identified for the item", example = "Heineken Bottle Cap")
    private String primaryName;

    @Schema(description = "Company that produces the item (e.g., brewery for beer caps)", example = "Heineken N.V.")
    private String company;

    @Schema(description = "Country of origin where the company is based", example = "Netherlands")
    private String country;

    @Schema(description = "Category of the item", example = "Beer")
    private String category;

    @Schema(description = "Detailed description of the item")
    private String description;

    @Schema(description = "Suggested tags for the item", example = "[\"beer\", \"green\", \"dutch\"]")
    private List<String> suggestedTags;

    @Schema(description = "Confidence score of the identification (0.0 - 1.0)", example = "0.95")
    private Double confidence;

    @Schema(description = "Rate limit information for the current user. Shows daily usage and remaining quota.")
    private RateLimitInfo rateLimit;
}
