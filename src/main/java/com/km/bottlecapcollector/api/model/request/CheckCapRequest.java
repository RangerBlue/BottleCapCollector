package com.km.bottlecapcollector.api.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
