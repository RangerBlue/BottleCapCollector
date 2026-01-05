package com.km.bottlecapcollector.gcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for checking if a bottle cap already exists in the collection.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckCapRequest {

    private String name;

    private String description;

    private List<String> tags;

    private String userId;

    private String collectionType;
}
