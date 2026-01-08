package com.km.bottlecapcollector.api.model.request;

import jakarta.validation.constraints.NotBlank;
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
public class CreateCollectionItemRequest {

    @NotBlank(message = "Name is required")
    private String name;
    private String description;
    private List<String> tags;
    private Map<String, String> customTags;
    private String collectionName;
}
