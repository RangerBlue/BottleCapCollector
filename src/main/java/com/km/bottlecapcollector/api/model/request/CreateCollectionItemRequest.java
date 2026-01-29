package com.km.bottlecapcollector.api.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create a new collection item")
public class CreateCollectionItemRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 30, message = "Name must be at most 30 characters")
    @Schema(description = "Name of the item", example = "Coca-Cola Classic", maxLength = 30, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 100, message = "Description must be at most 100 characters")
    @Schema(description = "Description of the item", example = "Red bottle cap from 2024 edition", maxLength = 100)
    private String description;

    @Schema(description = "List of tags for the item (truncated to 15 characters if longer)", example = "[\"beer\", \"craft\"]")
    private List<String> tags;

    @Schema(description = "Custom key-value tags (keys max 15 characters, values max 30 characters)", example = "{\"brand\": \"Pepsi\", \"year\": \"2024\"}")
    private Map<@Size(max = 15, message = "Custom tag key must be at most 15 characters") String,
                @Size(max = 30, message = "Custom tag value must be at most 30 characters") String> customTags;

    @Size(max = 20, message = "Collection name must be at most 20 characters")
    @Schema(description = "Name of the collection (created if doesn't exist)", example = "My Caps", maxLength = 20)
    private String collectionName;
}
